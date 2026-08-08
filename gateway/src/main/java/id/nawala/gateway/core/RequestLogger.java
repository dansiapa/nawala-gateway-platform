package id.nawala.gateway.core;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Component
@RequiredArgsConstructor
@Slf4j
public class RequestLogger {
    
    private final JdbcTemplate jdbcTemplate;
    private final Queue<LogEntry> logQueue = new ConcurrentLinkedQueue<>();
    private volatile boolean detailedLogging = false;
    
    public void logRequest(String requestId, String method, String path, 
                          Map<String, String> headers, String clientIp, Long routeId) {
        if (detailedLogging) {
            log.info("[{}] REQ {} {} from {} headers={}", 
                    requestId, method, path, clientIp, headers);
        } else {
            log.info("[{}] REQ {} {} from {}", requestId, method, path, clientIp);
        }
    }
    
    public void logResponse(String requestId, int statusCode, long latencyMs, String targetUrl) {
        log.info("[{}] RES {} {}ms -> {}", requestId, statusCode, latencyMs, targetUrl);
    }
    
    public void logError(String requestId, String error, String targetUrl) {
        log.error("[{}] ERR {} -> {}", requestId, error, targetUrl);
    }
    
    public void logWafBlock(String requestId, String reason, String clientIp, String path) {
        log.warn("[{}] WAF BLOCKED {} from {} path={}", requestId, reason, clientIp, path);
    }
    
    public void logRateLimit(String requestId, String clientKey, Long routeId) {
        log.warn("[{}] RATE LIMITED {} on route {}", requestId, clientKey, routeId);
    }
    
    public void logAuthFailure(String requestId, String reason, String clientIp) {
        log.warn("[{}] AUTH FAILED {} from {}", requestId, reason, clientIp);
    }
    
    public void logCircuitOpen(String requestId, String target) {
        log.warn("[{}] CIRCUIT OPEN for {}", requestId, target);
    }
    
    public void setDetailedLogging(boolean enabled) {
        this.detailedLogging = enabled;
        log.info("Detailed logging {}", enabled ? "enabled" : "disabled");
    }
    
    public record LogEntry(
        String requestId,
        String type,
        String message,
        long timestamp
    ) {}
}
