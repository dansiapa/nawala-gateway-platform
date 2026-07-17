package id.nawala.gateway.filter;

import id.nawala.gateway.logging.SecurityLogger;
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

/**
 * OAuth2 Bearer Token validation filter.
 * Validates tokens issued by the platform's OAuth2 server.
 */
@Component
@Slf4j
public class OAuthTokenFilter extends AbstractGatewayFilterFactory<OAuthTokenFilter.Config> {

    private final WebClient platformWebClient;

    public OAuthTokenFilter(@Value("${nawala.gateway.platform-url}") String platformUrl,
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
            String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return chain.filter(exchange);
            }

            String token = authHeader.substring(7);

            // Skip API keys (they start with nwl_)
            if (token.startsWith("nwl_")) {
                return chain.filter(exchange);
            }

            return platformWebClient
                    .post()
                    .uri("/internal/oauth/validate")
                    .bodyValue(Map.of("token", token))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .flatMap(response -> {
                        Boolean valid = (Boolean) response.get("valid");
                        if (Boolean.TRUE.equals(valid)) {
                            String clientId = (String) response.get("clientId");
                            String scopes = (String) response.get("scopes");
                            log.debug("OAuth token validated clientId={} scopes={}", clientId, scopes);
                            ServerWebExchange mutated = exchange.mutate()
                                    .request(r -> r
                                            .header("X-OAuth-Client", clientId)
                                            .header("X-OAuth-Scopes", scopes))
                                    .build();
                            return chain.filter(mutated);
                        } else {
                            SecurityLogger.log().warn("OAuth token rejected path={}",
                                    exchange.getRequest().getPath());
                            return unauthorized(exchange, "Invalid or expired token");
                        }
                    })
                    .onErrorResume(e -> {
                        log.error("Error validating OAuth token: {}", e.getMessage());
                        return chain.filter(exchange);
                    });
        };
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
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
