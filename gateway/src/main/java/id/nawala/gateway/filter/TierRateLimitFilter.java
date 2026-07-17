package id.nawala.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Tier-aware Rate Limit filter with per-minute/hour/day windows.
 */
@Component
@Slf4j
public class TierRateLimitFilter extends AbstractGatewayFilterFactory<TierRateLimitFilter.Config> {

    private static final long MINUTE_MS = 60_000;
    private static final long HOUR_MS = 3_600_000;
    private static final long DAY_MS = 86_400_000;

    private final Map<String, TierLimits> tierCache = new ConcurrentHashMap<>();
    private final Map<String, MultiWindow> clientWindows = new ConcurrentHashMap<>();
    private final WebClient platformWebClient;

    public TierRateLimitFilter(@Value("${nawala.gateway.platform-url}") String platformUrl,
                              @Value("${nawala.gateway.internal-secret:NawalaInternalSecretKey2024!}") String internalSecret) {
        super(Config.class);
        this.platformWebClient = WebClient.builder()
                .baseUrl(platformUrl)
                .defaultHeader("X-Internal-Secret", internalSecret)
                .build();
        tierCache.put("FREE", new TierLimits(30, 500, 5000, 5));
        tierCache.put("STARTER", new TierLimits(60, 2000, 20000, 10));
        tierCache.put("PROFESSIONAL", new TierLimits(120, 5000, 50000, 20));
        tierCache.put("ENTERPRISE", new TierLimits(600, 30000, 300000, 50));
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String clientId = resolveClientId(exchange);
            String tierName = resolveTierName(exchange);
            TierLimits limits = tierCache.getOrDefault(tierName, tierCache.get("FREE"));

            MultiWindow window = clientWindows.computeIfAbsent(clientId, k -> new MultiWindow());
            long now = System.currentTimeMillis();

            if (now - window.minuteStart.get() > MINUTE_MS) { window.minuteStart.set(now); window.minuteCount.set(0); }
            if (now - window.hourStart.get() > HOUR_MS) { window.hourStart.set(now); window.hourCount.set(0); }
            if (now - window.dayStart.get() > DAY_MS) { window.dayStart.set(now); window.dayCount.set(0); }

            int mUsed = window.minuteCount.incrementAndGet();
            int hUsed = window.hourCount.incrementAndGet();
            int dUsed = window.dayCount.incrementAndGet();

            if (mUsed > limits.perMinute) return reject(exchange, limits.perMinute, "minute");
            if (hUsed > limits.perHour) return reject(exchange, limits.perHour, "hour");
            if (dUsed > limits.perDay) return reject(exchange, limits.perDay, "day");

            exchange.getResponse().getHeaders().add("X-RateLimit-Tier", tierName);
            exchange.getResponse().getHeaders().add("X-RateLimit-Remaining-Minute",
                    String.valueOf(limits.perMinute - mUsed));
            return chain.filter(exchange);
        };
    }

    @SuppressWarnings("unchecked")
    public void refreshTiers() {
        platformWebClient.get().uri("/internal/rate-tiers").retrieve()
                .bodyToFlux(Map.class).collectList()
                .subscribe(tiers -> {
                    for (Map<String, Object> t : tiers) {
                        String name = (String) t.get("name");
                        tierCache.put(name, new TierLimits(
                                ((Number) t.get("requestsPerMinute")).intValue(),
                                ((Number) t.get("requestsPerHour")).intValue(),
                                ((Number) t.get("requestsPerDay")).intValue(),
                                ((Number) t.get("burstSize")).intValue()));
                    }
                }, e -> log.trace("Tier refresh failed: {}", e.getMessage()));
    }

    private String resolveClientId(ServerWebExchange exchange) {
        String k = exchange.getRequest().getHeaders().getFirst("X-Key-Prefix");
        if (k != null) return "key:" + k;
        String o = exchange.getRequest().getHeaders().getFirst("X-OAuth-Client");
        if (o != null) return "oauth:" + o;
        return "ip:" + (exchange.getRequest().getRemoteAddress() != null
                ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress() : "unknown");
    }

    private String resolveTierName(ServerWebExchange exchange) {
        String tier = exchange.getRequest().getHeaders().getFirst("X-Rate-Tier");
        return tier != null ? tier : "FREE";
    }

    private Mono<Void> reject(ServerWebExchange exchange, int limit, String win) {
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        exchange.getResponse().getHeaders().add("Content-Type", "application/json");
        String body = "{\"error\":\"Too Many Requests\",\"window\":\"" + win + "\",\"limit\":" + limit + "}";
        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(body.getBytes())));
    }

    private static class MultiWindow {
        final AtomicLong minuteStart = new AtomicLong(System.currentTimeMillis());
        final AtomicInteger minuteCount = new AtomicInteger(0);
        final AtomicLong hourStart = new AtomicLong(System.currentTimeMillis());
        final AtomicInteger hourCount = new AtomicInteger(0);
        final AtomicLong dayStart = new AtomicLong(System.currentTimeMillis());
        final AtomicInteger dayCount = new AtomicInteger(0);
    }

    private record TierLimits(int perMinute, int perHour, int perDay, int burstSize) {}

    public static class Config {
        private String defaultTier = "FREE";
        public String getDefaultTier() { return defaultTier; }
        public void setDefaultTier(String t) { this.defaultTier = t; }
    }
}
