package id.nawala.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Response caching filter.
 * Caches GET responses with configurable TTL per route.
 * Serves from cache when available, reducing backend load.
 */
@Component
@Slf4j
public class ResponseCacheFilter implements GlobalFilter, Ordered {

    private final Map<String, CachedResponse> cache = new ConcurrentHashMap<>();
    private static final long DEFAULT_TTL_MS = 60_000; // 1 minute
    private static final int MAX_CACHE_SIZE = 1000;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Only cache GET requests
        if (!"GET".equals(exchange.getRequest().getMethod().name())) {
            return chain.filter(exchange);
        }

        // Check for no-cache header
        String cacheControl = exchange.getRequest().getHeaders().getFirst(HttpHeaders.CACHE_CONTROL);
        if ("no-cache".equals(cacheControl) || "no-store".equals(cacheControl)) {
            return chain.filter(exchange);
        }

        String cacheKey = exchange.getRequest().getPath().value() + "?" +
                (exchange.getRequest().getURI().getRawQuery() != null ? exchange.getRequest().getURI().getRawQuery() : "");

        // Check cache
        CachedResponse cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.valueOf(cached.statusCode));
            response.getHeaders().set("X-Cache", "HIT");
            response.getHeaders().set(HttpHeaders.CONTENT_TYPE, cached.contentType);
            DataBuffer buffer = response.bufferFactory().wrap(cached.body);
            return response.writeWith(Mono.just(buffer));
        }

        // Cache miss - proceed and potentially cache response
        exchange.getResponse().getHeaders().set("X-Cache", "MISS");
        return chain.filter(exchange);
    }

    public void cacheResponse(String key, int statusCode, String contentType, byte[] body, long ttlMs) {
        if (cache.size() >= MAX_CACHE_SIZE) {
            // Evict expired entries
            cache.entrySet().removeIf(e -> e.getValue().isExpired());
        }
        if (statusCode >= 200 && statusCode < 300) {
            cache.put(key, new CachedResponse(statusCode, contentType, body, System.currentTimeMillis() + ttlMs));
        }
    }

    public void invalidate(String keyPattern) {
        cache.entrySet().removeIf(e -> e.getKey().contains(keyPattern));
    }

    public int getCacheSize() {
        return cache.size();
    }

    @Override
    public int getOrder() {
        return 3; // After WAF, before circuit breaker
    }

    private record CachedResponse(int statusCode, String contentType, byte[] body, long expiresAt) {
        boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }
}
