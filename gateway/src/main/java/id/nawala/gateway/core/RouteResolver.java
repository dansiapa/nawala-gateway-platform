package id.nawala.gateway.core;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class RouteResolver {

    private final JdbcTemplate jdbcTemplate;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    
    private volatile List<RouteDefinition> routes = new ArrayList<>();
    private final Map<String, RouteDefinition> routeCache = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void init() {
        loadRoutes();
        // Refresh routes every 30 seconds
        Executors.newSingleThreadScheduledExecutor()
                .scheduleAtFixedRate(this::loadRoutes, 30, 30, TimeUnit.SECONDS);
    }
    
    public void loadRoutes() {
        try {
            List<RouteDefinition> newRoutes = jdbcTemplate.query(
                "SELECT id, name, method, path, target_url, auth_required, rate_limit_enabled, " +
                "rate_limit_per_minute, payload_encryption, load_balanced, lb_strategy, timeout_seconds " +
                "FROM api_routes WHERE active = true ORDER BY priority DESC, path DESC",
                (rs, rowNum) -> RouteDefinition.builder()
                        .id(rs.getLong("id"))
                        .name(rs.getString("name"))
                        .method(rs.getString("method"))
                        .path(rs.getString("path"))
                        .targetUrl(rs.getString("target_url"))
                        .authRequired(rs.getBoolean("auth_required"))
                        .rateLimitEnabled(rs.getBoolean("rate_limit_enabled"))
                        .rateLimitPerMinute(rs.getInt("rate_limit_per_minute"))
                        .payloadEncryption(rs.getBoolean("payload_encryption"))
                        .loadBalanced(rs.getBoolean("load_balanced"))
                        .lbStrategy(rs.getString("lb_strategy"))
                        .timeoutSeconds(rs.getInt("timeout_seconds"))
                        .build()
            );
            
            // Load targets for load balanced routes
            for (RouteDefinition route : newRoutes) {
                if (route.isLoadBalanced()) {
                    List<TargetDefinition> targets = jdbcTemplate.query(
                        "SELECT id, url, weight, healthy FROM route_targets WHERE route_id = ? AND healthy = true",
                        (rs, rowNum) -> TargetDefinition.builder()
                                .id(rs.getLong("id"))
                                .url(rs.getString("url"))
                                .weight(rs.getInt("weight"))
                                .healthy(rs.getBoolean("healthy"))
                                .build(),
                        route.getId()
                    );
                    route.setTargets(targets);
                }
            }
            
            this.routes = newRoutes;
            this.routeCache.clear();
            log.info("Loaded {} active routes", newRoutes.size());
        } catch (Exception e) {
            log.error("Failed to load routes: {}", e.getMessage());
        }
    }
    
    public Optional<RouteDefinition> resolve(String method, String path) {
        String cacheKey = method + ":" + path;
        
        RouteDefinition cached = routeCache.get(cacheKey);
        if (cached != null) {
            return Optional.of(cached);
        }
        
        for (RouteDefinition route : routes) {
            if (matchesRoute(route, method, path)) {
                routeCache.put(cacheKey, route);
                return Optional.of(route);
            }
        }
        
        return Optional.empty();
    }
    
    private boolean matchesRoute(RouteDefinition route, String method, String path) {
        // Check method (wildcard * matches all)
        if (!"*".equals(route.getMethod()) && !route.getMethod().equalsIgnoreCase(method)) {
            return false;
        }
        
        // Check path with Ant-style pattern matching
        return pathMatcher.match(route.getPath(), path);
    }
    
    public Map<String, String> extractPathVariables(String pattern, String path) {
        return pathMatcher.extractUriTemplateVariables(pattern, path);
    }
    
    public void refreshRoutes() {
        loadRoutes();
    }
}
