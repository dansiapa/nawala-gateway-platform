package id.nawala.gateway.config;

import id.nawala.gateway.filter.ApiKeyAuthFilter;
import id.nawala.gateway.filter.JwtAuthFilter;
import id.nawala.gateway.filter.RateLimitFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@Configuration
@RequiredArgsConstructor
@EnableScheduling
@Slf4j
public class GatewayConfig {

    private final ApiKeyAuthFilter apiKeyAuthFilter;
    private final JwtAuthFilter jwtAuthFilter;
    private final RateLimitFilter rateLimitFilter;

    @Value("${nawala.gateway.platform-url}")
    private String platformUrl;

    @Value("${nawala.gateway.internal-secret:NawalaInternalSecretKey2024!}")
    private String internalSecret;

    private final List<Map<String, Object>> cachedRoutes = new CopyOnWriteArrayList<>();

    @Bean
    public WebClient platformWebClient() {
        return WebClient.builder()
                .baseUrl(platformUrl)
                .defaultHeader("X-Internal-Secret", internalSecret)
                .build();
    }

    @Bean
    public RouteLocator routeLocator(RouteLocatorBuilder builder) {
        RouteLocatorBuilder.Builder routes = builder.routes();

        // Auth service route (always available)
        routes.route("auth-service", r -> r
                .path("/auth/**")
                .uri(platformUrl)
        );

        // Dynamic routes from platform's registered routes
        routes.route("dynamic-routes", r -> r
                .path("/api/**")
                .filters(f -> f
                        .filter(rateLimitFilter.apply(new RateLimitFilter.Config()))
                        .filter(apiKeyAuthFilter.apply(new ApiKeyAuthFilter.Config()))
                        .filter(jwtAuthFilter.apply(new JwtAuthFilter.Config()))
                )
                .uri(platformUrl)
        );

        return routes.build();
    }

    @Scheduled(fixedDelay = 30000, initialDelay = 5000)
    @SuppressWarnings("unchecked")
    public void refreshRoutes() {
        try {
            platformWebClient()
                    .get()
                    .uri("/internal/routes")
                    .retrieve()
                    .bodyToFlux(Map.class)
                    .collectList()
                    .onErrorResume(e -> {
                        log.warn("Failed to refresh routes from platform: {}", e.getMessage());
                        return Mono.just(List.of());
                    })
                    .subscribe(routes -> {
                        if (!routes.isEmpty()) {
                            cachedRoutes.clear();
                            for (Map raw : routes) {
                                cachedRoutes.add((Map<String, Object>) raw);
                            }
                            log.debug("Refreshed {} routes from platform", routes.size());
                        }
                    });
        } catch (Exception e) {
            log.warn("Error refreshing routes: {}", e.getMessage());
        }
    }

    public List<Map<String, Object>> getCachedRoutes() {
        return cachedRoutes;
    }
}

