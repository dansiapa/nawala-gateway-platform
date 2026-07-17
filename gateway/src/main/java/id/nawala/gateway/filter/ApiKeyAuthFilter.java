package id.nawala.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
@Slf4j
public class ApiKeyAuthFilter extends AbstractGatewayFilterFactory<ApiKeyAuthFilter.Config> {

    private static final String API_KEY_HEADER = "X-API-Key";

    private final WebClient platformWebClient;

    public ApiKeyAuthFilter(@Value("${nawala.gateway.platform-url}") String platformUrl,
                            @Value("${nawala.gateway.internal-secret:NawalaInternalSecretKey2024!}") String internalSecret) {
        super(Config.class);
        this.platformWebClient = WebClient.builder()
                .baseUrl(platformUrl)
                .defaultHeader("X-Internal-Secret", internalSecret)
                .build();
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String apiKey = exchange.getRequest().getHeaders().getFirst(API_KEY_HEADER);

            if (apiKey == null || apiKey.isBlank()) {
                String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    // Let JWT filter handle this
                    return chain.filter(exchange);
                }
                return unauthorized(exchange, "Missing API Key or Bearer token");
            }

            if (!apiKey.startsWith("nwl_")) {
                return unauthorized(exchange, "Invalid API Key format");
            }

            // Validate key against platform
            return platformWebClient
                    .post()
                    .uri("/internal/keys/validate")
                    .bodyValue(Map.of("key", apiKey))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .flatMap(response -> {
                        Boolean valid = (Boolean) response.get("valid");
                        if (Boolean.TRUE.equals(valid)) {
                            log.debug("API Key validated: prefix={}", response.get("prefix"));
                            ServerWebExchange mutatedExchange = exchange.mutate()
                                    .request(r -> r.header("X-Key-Prefix", String.valueOf(response.get("prefix"))))
                                    .build();
                            return chain.filter(mutatedExchange);
                        } else {
                            return unauthorized(exchange, "Invalid or expired API key");
                        }
                    })
                    .onErrorResume(e -> {
                        log.error("Error validating API key: {}", e.getMessage());
                        log.warn("Platform unavailable, falling back to prefix validation");
                        return chain.filter(exchange);
                    });
        };
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        log.warn("Unauthorized request: {} - {}", exchange.getRequest().getPath(), message);
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().add("Content-Type", "application/json");
        String body = "{\"error\":\"Unauthorized\",\"message\":\"" + message + "\"}";
        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(body.getBytes()))
        );
    }

    public static class Config {
    }
}

