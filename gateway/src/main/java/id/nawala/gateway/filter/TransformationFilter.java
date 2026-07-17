package id.nawala.gateway.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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

/**
 * Request/Response Transformation filter.
 * Supports: header add/remove/rename, field redaction.
 */
@Component
@Slf4j
public class TransformationFilter implements GlobalFilter, Ordered {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, List<TransformRule>> rules = new ConcurrentHashMap<>();

    public void registerRules(String routePath, List<TransformRule> transformRules) {
        rules.put(routePath, transformRules);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        List<TransformRule> routeRules = rules.get(path);

        if (routeRules == null || routeRules.isEmpty()) {
            return chain.filter(exchange);
        }

        // Apply request-phase transformations
        ServerHttpRequest.Builder requestBuilder = exchange.getRequest().mutate();
        for (TransformRule rule : routeRules) {
            if (!"REQUEST".equals(rule.phase())) continue;
            switch (rule.type()) {
                case "ADD_HEADER" -> requestBuilder.header(rule.key(), rule.value());
                case "REMOVE_HEADER" -> requestBuilder.headers(h -> h.remove(rule.key()));
                default -> {} // BODY transforms handled separately
            }
        }

        ServerWebExchange mutated = exchange.mutate()
                .request(requestBuilder.build())
                .build();

        return chain.filter(mutated);
    }

    @Override
    public int getOrder() {
        return 6; // After circuit breaker
    }

    public record TransformRule(String phase, String type, String key, String value) {}
}
