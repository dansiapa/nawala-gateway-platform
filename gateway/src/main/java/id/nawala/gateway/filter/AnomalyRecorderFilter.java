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

import java.util.Map;

/**
 * Global post-filter that records each request to the anomaly detection service.
 * Runs after the response is sent, so it doesn't impact latency.
 */
@Component
@Slf4j
public class AnomalyRecorderFilter implements GlobalFilter, Ordered {

    private final WebClient platformWebClient;

    public AnomalyRecorderFilter(@Value("${nawala.gateway.platform-url}") String platformUrl,
                                 @Value("${nawala.gateway.internal-secret:NawalaInternalSecretKey2024!}") String internalSecret) {
        this.platformWebClient = WebClient.builder()
                .baseUrl(platformUrl)
                .defaultHeader("X-Internal-Secret", internalSecret)
                .build();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            try {
                String sourceIp = exchange.getRequest().getRemoteAddress() != null
                        ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                        : "unknown";
                String apiKeyPrefix = exchange.getRequest().getHeaders().getFirst("X-Key-Prefix");
                String path = exchange.getRequest().getPath().value();
                int status = exchange.getResponse().getStatusCode() != null
                        ? exchange.getResponse().getStatusCode().value() : 200;

                platformWebClient.post()
                        .uri("/internal/anomaly/record")
                        .bodyValue(Map.of(
                                "sourceIp", sourceIp,
                                "apiKeyPrefix", apiKeyPrefix != null ? apiKeyPrefix : "",
                                "path", path,
                                "status", status
                        ))
                        .retrieve()
                        .bodyToMono(Void.class)
                        .subscribe(
                                null,
                                e -> log.trace("Anomaly record failed: {}", e.getMessage())
                        );
            } catch (Exception e) {
                log.trace("Error recording anomaly: {}", e.getMessage());
            }
        }));
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE; // Run last
    }
}
