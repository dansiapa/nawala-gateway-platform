package id.nawala.platform.service.impl;

import id.nawala.platform.model.Plugin;
import id.nawala.platform.model.User;
import id.nawala.platform.repository.PluginRepository;
import id.nawala.platform.repository.UserRepository;
import id.nawala.platform.service.PluginService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.SimpleBindings;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PluginServiceImpl implements PluginService {

    private final PluginRepository pluginRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public Plugin create(Long userId, String name, String description, String hookType,
                         String script, Long routeId, int priority) {
        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Plugin plugin = Plugin.builder()
                .owner(owner).name(name).description(description)
                .hookType(hookType).script(script).routeId(routeId)
                .priority(priority).active(true)
                .build();
        return pluginRepository.save(plugin);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Plugin> getByUser(Long userId) {
        return pluginRepository.findByOwnerId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Plugin> getActiveByHook(String hookType, Long routeId) {
        if (routeId != null) {
            return pluginRepository.findByRouteIdAndHookTypeAndActiveTrueOrderByPriorityAsc(routeId, hookType);
        }
        return pluginRepository.findByHookTypeAndActiveTrueOrderByPriorityAsc(hookType);
    }

    @Override
    @Transactional
    public PluginExecutionResult execute(Plugin plugin, Map<String, Object> context) {
        long start = System.currentTimeMillis();
        try {
            ScriptEngineManager manager = new ScriptEngineManager();
            ScriptEngine engine = manager.getEngineByName("nashorn");
            if (engine == null) {
                engine = manager.getEngineByName("js");
            }
            if (engine == null) {
                return new PluginExecutionResult(false, null, 0, "No JavaScript engine available");
            }
            SimpleBindings bindings = new SimpleBindings();
            bindings.putAll(context);
            bindings.put("log", log);
            Object result = engine.eval(plugin.getScript(), bindings);
            long duration = System.currentTimeMillis() - start;
            plugin.setExecutionCount(plugin.getExecutionCount() + 1);
            plugin.setLastExecutedAt(LocalDateTime.now());
            plugin.setAvgExecutionTimeMs(duration);
            pluginRepository.save(plugin);
            return new PluginExecutionResult(true, result != null ? result.toString() : null, duration, null);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            plugin.setExecutionCount(plugin.getExecutionCount() + 1);
            plugin.setErrorCount(plugin.getErrorCount() + 1);
            plugin.setLastExecutedAt(LocalDateTime.now());
            pluginRepository.save(plugin);
            log.warn("Plugin execution failed name={} error={}", plugin.getName(), e.getMessage());
            return new PluginExecutionResult(false, null, duration, e.getMessage());
        }
    }

    @Override
    @Transactional
    public void delete(Long pluginId) {
        pluginRepository.deleteById(pluginId);
    }

    @Override
    @Transactional
    public void toggle(Long pluginId, boolean active) {
        pluginRepository.findById(pluginId).ifPresent(p -> {
            p.setActive(active);
            pluginRepository.save(p);
        });
    }
}
