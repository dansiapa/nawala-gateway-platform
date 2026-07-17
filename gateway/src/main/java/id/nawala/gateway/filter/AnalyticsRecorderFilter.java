package id.nawala.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * Records analytics data (response time, status, sizes) to platform service.
 * Runs as the very last filter to capture actual response metadata.
 */
@Component
@Slf4j
public class AnalyticsRecorderFilter implements GlobalFilter, Ordered {

    private final WebClient platformWebClient;

    public AnalyticsRecorderFilter(@Value("${nawala.gateway.platform-url}") String platformUrl,
                                   @Value("${nawala.gateway.internal-secret:NawalaInternalSecretKey2024!}") String internalSecret) {
        this.platformWebClient = WebClient.builder()
                .baseUrl(platformUrl)
                .defaultHeader("X-Internal-Secret", internalSecret)
                .build();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long startTime = System.currentTimeMillis();
        exchange.getAttributes().put("analyticsStartTime", startTime);

        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            try {
                long duration = System.currentTimeMillis() - startTime;
                String sourceIp = exchange.getRequest().getRemoteAddress() != null
                        ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                        : "unknown";
                String apiKeyPrefix = exchange.getRequest().getHeaders().getFirst("X-Key-Prefix");
                String method = exchange.getRequest().getMethod().name();
                String path = exchange.getRequest().getPath().value();
                int status = exchange.getResponse().getStatusCode() != null
                        ? exchange.getResponse().getStatusCode().value() : 200;

                Map<String, Object> data = new HashMap<>();
                data.put("sourceIp", sourceIp);
                data.put("apiKeyPrefix", apiKeyPrefix != null ? apiKeyPrefix : "");
                data.put("method", method);
                data.put("path", path);
                data.put("statusCode", status);
                data.put("responseTimeMs", duration);
                data.put("requestSize", exchange.getRequest().getHeaders().getContentLength());
                data.put("responseSize", 0L);

                platformWebClient.post()
                        .uri("/internal/analytics/record")
                        .bodyValue(data)
                        .retrieve()
                        .bodyToMono(Void.class)
                        .subscribe(null, e -> log.trace("Analytics record failed: {}", e.getMessage()));
            } catch (Exception e) {
                log.trace("Error recording analytics: {}", e.getMessage());
            }
        }));
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 1; // Just before anomaly recorder
    }
}
