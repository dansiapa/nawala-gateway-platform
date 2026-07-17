package id.nawala.gateway.filter;

import id.nawala.gateway.circuitbreaker.CircuitBreakerRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;

/**
 * Circuit Breaker filter that prevents requests to failing targets.
 * Returns 503 Service Unavailable when circuit is OPEN.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CircuitBreakerFilter implements GlobalFilter, Ordered {

    private final CircuitBreakerRegistry registry;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        URI targetUri = exchange.getAttribute(GATEWAY_REQUEST_URL_ATTR);
        if (targetUri == null) {
            return chain.filter(exchange);
        }

        String targetKey = targetUri.getHost() + ":" + targetUri.getPort();

        if (!registry.isCallPermitted(targetKey)) {
            log.warn("Circuit OPEN - blocking request to {}", targetKey);
            exchange.getResponse().setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
            exchange.getResponse().getHeaders().set("X-Circuit-State", "OPEN");
            return exchange.getResponse().setComplete();
        }

        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            HttpStatus status = (HttpStatus) exchange.getResponse().getStatusCode();
            if (status != null && status.is5xxServerError()) {
                registry.recordFailure(targetKey);
            } else {
                registry.recordSuccess(targetKey);
            }
        }));
    }

    @Override
    public int getOrder() {
        return 5; // After auth, before response
    }
}
