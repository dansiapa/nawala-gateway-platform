package id.nawala.gateway.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Reactive MDC + Access Log filter for the gateway.
 * Populates traceId, clientIp, apiKey into MDC and logs every
 * request to the dedicated access log file.
 */
@Component
public class GatewayAccessLogFilter implements GlobalFilter, Ordered {

    private static final Logger ACCESS_LOG = LoggerFactory.getLogger(AccessLogger.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long startTime = System.currentTimeMillis();
        ServerHttpRequest request = exchange.getRequest();

        // Generate trace ID
        String traceId = request.getHeaders().getFirst("X-Trace-Id");
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }

        String clientIp = request.getRemoteAddress() != null
                ? request.getRemoteAddress().getAddress().getHostAddress()
                : "unknown";
        String apiKey = request.getHeaders().getFirst("X-API-Key");
        String keyPrefix = apiKey != null && apiKey.length() >= 8
                ? apiKey.substring(0, 8) : "-";
        String method = request.getMethod() != null ? request.getMethod().name() : "?";
        String path = request.getPath().value();

        // Store in exchange attributes for downstream filters
        exchange.getAttributes().put("traceId", traceId);
        exchange.getAttributes().put("clientIp", clientIp);
        exchange.getAttributes().put("startTime", startTime);

        // Add trace ID to response header
        String finalTraceId = traceId;
        exchange.getResponse().getHeaders().add("X-Trace-Id", traceId);
        // Store key prefix for anomaly recorder
        exchange.getRequest().mutate().header("X-Key-Prefix", keyPrefix).build();

        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            long duration = System.currentTimeMillis() - startTime;
            int status = exchange.getResponse().getStatusCode() != null
                    ? exchange.getResponse().getStatusCode().value() : 0;

            try {
                MDC.put("traceId", finalTraceId);
                MDC.put("clientIp", clientIp);
                MDC.put("apiKey", keyPrefix);
                ACCESS_LOG.info("{} {} {} {}ms ip={} key={}",
                        method, path, status, duration, clientIp, keyPrefix);
            } finally {
                MDC.clear();
            }
        }));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE; // Run first
    }
}
