package id.nawala.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Slf4j
public class RateLimitFilter extends AbstractGatewayFilterFactory<RateLimitFilter.Config> {

    private static final int DEFAULT_LIMIT = 60;
    private static final long WINDOW_MS = 60_000;

    private final Map<String, RateWindow> clientWindows = new ConcurrentHashMap<>();

    public RateLimitFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String clientId = resolveClientId(exchange);
            int limit = config.getLimit() > 0 ? config.getLimit() : DEFAULT_LIMIT;

            RateWindow window = clientWindows.computeIfAbsent(clientId, k -> new RateWindow());

            long now = System.currentTimeMillis();
            if (now - window.windowStart.get() > WINDOW_MS) {
                window.windowStart.set(now);
                window.requestCount.set(0);
            }

            int currentCount = window.requestCount.incrementAndGet();
            if (currentCount > limit) {
                log.warn("Rate limit exceeded for client: {} ({}/{})", clientId, currentCount, limit);
                return tooManyRequests(exchange, limit);
            }

            exchange.getResponse().getHeaders().add("X-RateLimit-Limit", String.valueOf(limit));
            exchange.getResponse().getHeaders().add("X-RateLimit-Remaining", String.valueOf(limit - currentCount));

            return chain.filter(exchange);
        };
    }

    private String resolveClientId(ServerWebExchange exchange) {
        String apiKey = exchange.getRequest().getHeaders().getFirst("X-API-Key");
        if (apiKey != null && !apiKey.isBlank()) {
            return "key:" + apiKey.substring(0, Math.min(8, apiKey.length()));
        }
        String ip = exchange.getRequest().getRemoteAddress() != null
                ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                : "unknown";
        return "ip:" + ip;
    }

    private Mono<Void> tooManyRequests(ServerWebExchange exchange, int limit) {
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        exchange.getResponse().getHeaders().add("Content-Type", "application/json");
        exchange.getResponse().getHeaders().add("X-RateLimit-Limit", String.valueOf(limit));
        exchange.getResponse().getHeaders().add("X-RateLimit-Remaining", "0");
        String body = "{\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded. Try again later.\"}";
        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(body.getBytes()))
        );
    }

    private static class RateWindow {
        final AtomicLong windowStart = new AtomicLong(System.currentTimeMillis());
        final AtomicInteger requestCount = new AtomicInteger(0);
    }

    public static class Config {
        private int limit = DEFAULT_LIMIT;

        public int getLimit() { return limit; }
        public void setLimit(int limit) { this.limit = limit; }
    }
}
