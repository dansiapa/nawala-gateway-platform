package id.nawala.gateway.core;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class HealthChecker {
    
    private final JdbcTemplate jdbcTemplate;
    private final WebClient.Builder webClientBuilder;
    private final Map<Long, Boolean> healthStatus = new ConcurrentHashMap<>();
    
    @Scheduled(fixedRate = 30000) // Every 30 seconds
    public void checkHealth() {
        try {
            List<TargetHealth> targets = jdbcTemplate.query(
                "SELECT rt.id, rt.url, rt.health_check_url, ar.name as route_name " +
                "FROM route_targets rt JOIN api_routes ar ON rt.route_id = ar.id " +
                "WHERE ar.active = true",
                (rs, rowNum) -> new TargetHealth(
                    rs.getLong("id"),
                    rs.getString("url"),
                    rs.getString("health_check_url"),
                    rs.getString("route_name")
                )
            );
            
            for (TargetHealth target : targets) {
                checkTargetHealth(target);
            }
        } catch (Exception e) {
            log.error("Health check failed: {}", e.getMessage());
        }
    }
    
    private void checkTargetHealth(TargetHealth target) {
        String healthUrl = target.healthCheckUrl() != null ? target.healthCheckUrl() : target.url();
        
        webClientBuilder.build()
            .get()
            .uri(healthUrl)
            .retrieve()
            .toBodilessEntity()
            .timeout(Duration.ofSeconds(5))
            .subscribe(
                response -> {
                    boolean healthy = response.getStatusCode().is2xxSuccessful();
                    updateHealth(target.id(), healthy);
                },
                error -> {
                    log.warn("Health check failed for {}: {}", target.url(), error.getMessage());
                    updateHealth(target.id(), false);
                }
            );
    }
    
    private void updateHealth(Long targetId, boolean healthy) {
        Boolean previous = healthStatus.put(targetId, healthy);
        
        if (previous == null || previous != healthy) {
            log.info("Target {} health changed to {}", targetId, healthy ? "HEALTHY" : "UNHEALTHY");
            
            try {
                jdbcTemplate.update(
                    "UPDATE route_targets SET healthy = ?, last_health_check = NOW() WHERE id = ?",
                    healthy, targetId
                );
            } catch (Exception e) {
                log.error("Failed to update health status: {}", e.getMessage());
            }
        }
    }
    
    public boolean isHealthy(Long targetId) {
        return healthStatus.getOrDefault(targetId, true);
    }
    
    public Map<Long, Boolean> getAllHealthStatus() {
        return Map.copyOf(healthStatus);
    }
    
    private record TargetHealth(Long id, String url, String healthCheckUrl, String routeName) {}
}
