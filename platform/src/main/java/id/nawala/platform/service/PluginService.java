package id.nawala.platform.service;

import id.nawala.platform.model.Plugin;
import java.util.List;
import java.util.Map;

/**
 * Plugin system service for extensibility hooks.
 */
public interface PluginService {

    Plugin create(Long userId, String name, String description, String hookType,
                  String script, Long routeId, int priority);

    List<Plugin> getByUser(Long userId);

    List<Plugin> getActiveByHook(String hookType, Long routeId);

    PluginExecutionResult execute(Plugin plugin, Map<String, Object> context);

    void delete(Long pluginId);

    void toggle(Long pluginId, boolean active);

    record PluginExecutionResult(boolean success, String output, long durationMs, String error) {}
}
