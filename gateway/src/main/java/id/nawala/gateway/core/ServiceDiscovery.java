package id.nawala.gateway.core;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class ServiceDiscovery {
    
    private final JdbcTemplate jdbcTemplate;
    
    // service name -> list of instances
    private final Map<String, List<ServiceInstance>> services = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void init() {
        refreshServices();
    }
    
    @Scheduled(fixedRate = 60000) // Refresh every minute
    public void refreshServices() {
        try {
            // Load from database
            List<ServiceInstance> instances = jdbcTemplate.query(
                "SELECT id, service_name, url, metadata, healthy, last_heartbeat " +
                "FROM service_registry WHERE healthy = true",
                (rs, rowNum) -> ServiceInstance.builder()
                        .id(rs.getLong("id"))
                        .serviceName(rs.getString("service_name"))
                        .url(rs.getString("url"))
                        .metadata(rs.getString("metadata"))
                        .healthy(rs.getBoolean("healthy"))
                        .lastHeartbeat(rs.getTimestamp("last_heartbeat") != null ? 
                                      rs.getTimestamp("last_heartbeat").getTime() : 0)
                        .build()
            );
            
            // Group by service name
            Map<String, List<ServiceInstance>> newServices = new HashMap<>();
            for (ServiceInstance instance : instances) {
                newServices.computeIfAbsent(instance.getServiceName(), k -> new ArrayList<>())
                          .add(instance);
            }
            
            services.clear();
            services.putAll(newServices);
            
            log.debug("Discovered {} services with {} total instances", 
                     services.size(), instances.size());
        } catch (Exception e) {
            log.error("Failed to refresh services: {}", e.getMessage());
        }
    }
    
    public Optional<ServiceInstance> discover(String serviceName) {
        List<ServiceInstance> instances = services.get(serviceName);
        if (instances == null || instances.isEmpty()) {
            return Optional.empty();
        }
        
        // Simple round-robin selection
        int index = Math.abs(serviceName.hashCode() + (int)(System.currentTimeMillis() / 1000)) % instances.size();
        return Optional.of(instances.get(index));
    }
    
    public List<ServiceInstance> getAllInstances(String serviceName) {
        return services.getOrDefault(serviceName, Collections.emptyList());
    }
    
    public Set<String> getServiceNames() {
        return services.keySet();
    }
    
    public void register(String serviceName, String url, String metadata) {
        try {
            jdbcTemplate.update(
                "INSERT INTO service_registry (service_name, url, metadata, healthy, last_heartbeat) " +
                "VALUES (?, ?, ?, true, NOW()) " +
                "ON CONFLICT (service_name, url) DO UPDATE SET " +
                "metadata = EXCLUDED.metadata, healthy = true, last_heartbeat = NOW()",
                serviceName, url, metadata
            );
            log.info("Registered service: {} at {}", serviceName, url);
            refreshServices();
        } catch (Exception e) {
            log.error("Failed to register service: {}", e.getMessage());
        }
    }
    
    public void deregister(String serviceName, String url) {
        try {
            jdbcTemplate.update(
                "UPDATE service_registry SET healthy = false WHERE service_name = ? AND url = ?",
                serviceName, url
            );
            log.info("Deregistered service: {} at {}", serviceName, url);
            refreshServices();
        } catch (Exception e) {
            log.error("Failed to deregister service: {}", e.getMessage());
        }
    }
    
    public void heartbeat(String serviceName, String url) {
        try {
            jdbcTemplate.update(
                "UPDATE service_registry SET last_heartbeat = NOW(), healthy = true " +
                "WHERE service_name = ? AND url = ?",
                serviceName, url
            );
        } catch (Exception e) {
            log.error("Heartbeat failed: {}", e.getMessage());
        }
    }
    
    // Mark unhealthy services that haven't sent heartbeat
    @Scheduled(fixedRate = 30000)
    public void markUnhealthyServices() {
        try {
            int updated = jdbcTemplate.update(
                "UPDATE service_registry SET healthy = false " +
                "WHERE last_heartbeat < NOW() - INTERVAL '60 seconds' AND healthy = true"
            );
            if (updated > 0) {
                log.warn("Marked {} services as unhealthy due to missing heartbeat", updated);
                refreshServices();
            }
        } catch (Exception e) {
            log.error("Failed to mark unhealthy services: {}", e.getMessage());
        }
    }
    
    @lombok.Data
    @lombok.Builder
    public static class ServiceInstance {
        private Long id;
        private String serviceName;
        private String url;
        private String metadata;
        private boolean healthy;
        private long lastHeartbeat;
    }
}
