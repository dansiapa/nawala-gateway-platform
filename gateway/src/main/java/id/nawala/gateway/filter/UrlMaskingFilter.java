package id.nawala.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * URL Masking filter. Translates public-facing masked paths
 * to actual internal target URLs, hiding real backend endpoints from clients.
 *
 * Example:
 *   Client calls: GET /public/v1/users
 *   Masked path:  /public/v1/users/**
 *   Real path:    /api/v1/users/**
 *   Target URL:   http://localhost:8081/api/users
 *
 * The real target URL is never exposed to the client.
 */
@Component
@Slf4j
public class UrlMaskingFilter implements GlobalFilter, Ordered {

    private final WebClient platformWebClient;
    private final List<MaskedRoute> maskedRoutes = new CopyOnWriteArrayList<>();
    private volatile long lastRefresh = 0;
    private static final long REFRESH_INTERVAL = 30_000;

    public UrlMaskingFilter(@Value("${nawala.gateway.platform-url}") String platformUrl,
                           @Value("${nawala.gateway.internal-secret:NawalaInternalSecretKey2024!}") String internalSecret) {
        this.platformWebClient = WebClient.builder()
                .baseUrl(platformUrl)
                .defaultHeader("X-Internal-Secret", internalSecret)
                .build();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String requestPath = exchange.getRequest().getPath().value();

        // Refresh routes cache if stale
        long now = System.currentTimeMillis();
        if (now - lastRefresh > REFRESH_INTERVAL) {
            lastRefresh = now;
            platformWebClient.get()
                    .uri("/internal/routes")
                    .retrieve()
                    .bodyToFlux(Map.class)
                    .collectList()
                    .subscribe(routes -> {
                        maskedRoutes.clear();
                        for (Map<String, Object> route : routes) {
                            String maskedPath = (String) route.get("maskedPath");
                            String realPath = (String) route.get("path");
                            String targetUrl = (String) route.get("targetUrl");
                            if (maskedPath != null && !maskedPath.isBlank()) {
                                maskedRoutes.add(new MaskedRoute(maskedPath, realPath, targetUrl));
                            }
                        }
                        if (!maskedRoutes.isEmpty()) {
                            log.debug("Loaded {} masked routes", maskedRoutes.size());
                        }
                    }, e -> log.trace("Failed to refresh masked routes: {}", e.getMessage()));
        }

        // Check if request matches a masked path
        for (MaskedRoute route : maskedRoutes) {
            if (matchesPattern(requestPath, route.maskedPath)) {
                // Rewrite the request path to the real internal path
                String rewrittenPath = rewritePath(requestPath, route.maskedPath, route.realPath);
                URI newUri = URI.create(route.targetUrl + rewrittenPath);

                ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                        .uri(newUri)
                        .path(rewrittenPath)
                        .build();

                ServerWebExchange mutatedExchange = exchange.mutate()
                        .request(mutatedRequest)
                        .build();

                log.debug("URL masked: {} -> {}", requestPath, rewrittenPath);
                return chain.filter(mutatedExchange);
            }
        }

        return chain.filter(exchange);
    }

    private boolean matchesPattern(String path, String pattern) {
        if (pattern.endsWith("/**")) {
            String prefix = pattern.substring(0, pattern.length() - 3);
            return path.startsWith(prefix);
        }
        return path.equals(pattern);
    }

    private String rewritePath(String requestPath, String maskedPattern, String realPattern) {
        String maskedPrefix = maskedPattern.endsWith("/**")
                ? maskedPattern.substring(0, maskedPattern.length() - 3) : maskedPattern;
        String realPrefix = realPattern.endsWith("/**")
                ? realPattern.substring(0, realPattern.length() - 3) : realPattern;

        if (requestPath.startsWith(maskedPrefix)) {
            String suffix = requestPath.substring(maskedPrefix.length());
            return realPrefix + suffix;
        }
        return realPrefix;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 2; // After threat block, before other filters
    }

    private record MaskedRoute(String maskedPath, String realPath, String targetUrl) {}
}
