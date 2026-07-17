package id.nawala.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Load Balancer filter that distributes requests across multiple targets.
 * Supports round-robin and weighted strategies.
 */
@Component
@Slf4j
public class LoadBalancerFilter implements GlobalFilter, Ordered {

    private final Map<String, AtomicInteger> roundRobinCounters = new ConcurrentHashMap<>();
    private final Map<String, List<WeightedTarget>> targetRegistry = new ConcurrentHashMap<>();

    public void registerTargets(String routePath, List<WeightedTarget> targets) {
        targetRegistry.put(routePath, targets);
        roundRobinCounters.putIfAbsent(routePath, new AtomicInteger(0));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        List<WeightedTarget> targets = targetRegistry.get(path);
        if (targets == null || targets.isEmpty()) {
            return chain.filter(exchange);
        }

        // Filter only healthy targets
        List<WeightedTarget> healthy = targets.stream()
                .filter(WeightedTarget::healthy)
                .toList();

        if (healthy.isEmpty()) {
            log.warn("No healthy targets for path={}", path);
            return chain.filter(exchange);
        }

        WeightedTarget selected = selectTarget(path, healthy);
        if (selected != null) {
            ServerHttpRequest mutated = exchange.getRequest().mutate()
                    .header("X-Target-Url", selected.url())
                    .header("X-Load-Balanced", "true")
                    .build();
            exchange = exchange.mutate().request(mutated).build();
        }

        return chain.filter(exchange);
    }

    private WeightedTarget selectTarget(String path, List<WeightedTarget> targets) {
        // Check for canary targets first
        for (WeightedTarget t : targets) {
            if (t.canary() && Math.random() * 100 < t.canaryPercentage()) {
                log.debug("Canary routing to {}", t.url());
                return t;
            }
        }

        // Round-robin for non-canary
        List<WeightedTarget> nonCanary = targets.stream()
                .filter(t -> !t.canary())
                .toList();
        if (nonCanary.isEmpty()) return targets.get(0);

        AtomicInteger counter = roundRobinCounters.computeIfAbsent(path, k -> new AtomicInteger(0));
        int index = Math.abs(counter.getAndIncrement()) % nonCanary.size();
        return nonCanary.get(index);
    }

    @Override
    public int getOrder() {
        return 4; // After cache, before circuit breaker
    }

    public record WeightedTarget(String url, int weight, boolean healthy, boolean canary, int canaryPercentage) {}
}
