package id.nawala.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Global pre-filter that checks if the source IP or API key is blocked
 * by the anomaly detection system before processing the request.
 */
@Component
@Slf4j
public class ThreatBlockFilter implements GlobalFilter, Ordered {

    private final WebClient platformWebClient;

    public ThreatBlockFilter(@Value("${nawala.gateway.platform-url}") String platformUrl,
                            @Value("${nawala.gateway.internal-secret:NawalaInternalSecretKey2024!}") String internalSecret) {
        this.platformWebClient = WebClient.builder()
                .baseUrl(platformUrl)
                .defaultHeader("X-Internal-Secret", internalSecret)
                .build();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        // Skip internal/health/auth paths
        if (path.startsWith("/auth") || path.startsWith("/internal") || path.startsWith("/actuator")) {
            return chain.filter(exchange);
        }

        String sourceIp = exchange.getRequest().getRemoteAddress() != null
                ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                : null;
        String apiKey = exchange.getRequest().getHeaders().getFirst("X-API-Key");
        String keyPrefix = apiKey != null && apiKey.length() >= 8 ? apiKey.substring(0, 8) : null;

        return platformWebClient.post()
                .uri("/internal/anomaly/check-block")
                .bodyValue(Map.of(
                        "sourceIp", sourceIp != null ? sourceIp : "",
                        "apiKeyPrefix", keyPrefix != null ? keyPrefix : ""
                ))
                .retrieve()
                .bodyToMono(Map.class)
                .flatMap(response -> {
                    Boolean blocked = (Boolean) response.get("blocked");
                    if (Boolean.TRUE.equals(blocked)) {
                        log.warn("Blocked request from ip={} key={}: threat detected", sourceIp, keyPrefix);
                        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                        exchange.getResponse().getHeaders().add("Content-Type", "application/json");
                        String body = "{\"error\":\"Forbidden\",\"message\":\"Your access has been temporarily blocked due to suspicious activity.\"}";
                        return exchange.getResponse().writeWith(
                                Mono.just(exchange.getResponse().bufferFactory().wrap(body.getBytes()))
                        );
                    }
                    return chain.filter(exchange);
                })
                .onErrorResume(e -> {
                    // If platform is unreachable, don't block - fail open
                    log.trace("Threat check unavailable: {}", e.getMessage());
                    return chain.filter(exchange);
                });
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1; // Run very early, right after routing
    }
}
