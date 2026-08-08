package id.nawala.gateway.core;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

@Component
@RequiredArgsConstructor
@Slf4j
public class AnalyticsCollector {
    
    private final JdbcTemplate jdbcTemplate;
    private final Queue<RequestMetric> metricsQueue = new ConcurrentLinkedQueue<>();
    
    // Real-time counters
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong totalErrors = new AtomicLong(0);
    private final AtomicLong totalLatencyMs = new AtomicLong(0);
    
    public void recordRequest(Long routeId, String method, String path, int statusCode, 
                              long latencyMs, String clientIp, Long apiKeyId) {
        totalRequests.incrementAndGet();
        totalLatencyMs.addAndGet(latencyMs);
        
        if (statusCode >= 400) {
            totalErrors.incrementAndGet();
        }
        
        metricsQueue.offer(new RequestMetric(
            routeId, method, path, statusCode, latencyMs, clientIp, apiKeyId,
            System.currentTimeMillis()
        ));
    }
    
    @Scheduled(fixedRate = 10000) // Flush every 10 seconds
    public void flushMetrics() {
        List<RequestMetric> batch = new ArrayList<>();
        RequestMetric metric;
        
        while ((metric = metricsQueue.poll()) != null && batch.size() < 1000) {
            batch.add(metric);
        }
        
        if (batch.isEmpty()) return;
        
        try {
            jdbcTemplate.batchUpdate(
                "INSERT INTO request_logs (route_id, method, path, status_code, latency_ms, " +
                "client_ip, api_key_id, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, NOW())",
                batch.stream().map(m -> new Object[]{
                    m.routeId(), m.method(), m.path(), m.statusCode(),
                    m.latencyMs(), m.clientIp(), m.apiKeyId()
                }).toList()
            );
            log.debug("Flushed {} metrics", batch.size());
        } catch (Exception e) {
            log.error("Failed to flush metrics: {}", e.getMessage());
            // Re-queue failed metrics
            metricsQueue.addAll(batch);
        }
    }
    
    // Update route statistics
    @Scheduled(fixedRate = 60000) // Every minute
    public void updateRouteStats() {
        try {
            jdbcTemplate.update(
                "UPDATE api_routes ar SET " +
                "request_count = (SELECT COUNT(*) FROM request_logs rl WHERE rl.route_id = ar.id), " +
                "error_count = (SELECT COUNT(*) FROM request_logs rl WHERE rl.route_id = ar.id AND rl.status_code >= 400), " +
                "avg_latency_ms = (SELECT AVG(latency_ms) FROM request_logs rl WHERE rl.route_id = ar.id)"
            );
        } catch (Exception e) {
            log.error("Failed to update route stats: {}", e.getMessage());
        }
    }
    
    public Stats getRealtimeStats() {
        long requests = totalRequests.get();
        long errors = totalErrors.get();
        long latency = totalLatencyMs.get();
        
        return new Stats(
            requests,
            errors,
            requests > 0 ? latency / requests : 0,
            requests > 0 ? (double)(requests - errors) / requests * 100 : 100
        );
    }
    
    public record RequestMetric(
        Long routeId, String method, String path, int statusCode,
        long latencyMs, String clientIp, Long apiKeyId, long timestamp
    ) {}
    
    public record Stats(long totalRequests, long totalErrors, long avgLatencyMs, double successRate) {}
}
