package id.nawala.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mock endpoint filter for API sandbox.
 * Serves mock responses without forwarding to real backend.
 */
@Component
@Slf4j
public class MockEndpointFilter implements GlobalFilter, Ordered {

    private final Map<String, MockResponse> mocks = new ConcurrentHashMap<>();

    public void registerMock(String method, String path, MockResponse response) {
        String key = method.toUpperCase() + ":" + path;
        mocks.put(key, response);
        log.info("Mock registered: {} {}", method, path);
    }

    public void removeMock(String method, String path) {
        mocks.remove(method.toUpperCase() + ":" + path);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String method = request.getMethod() != null ? request.getMethod().name() : "GET";
        String path = request.getPath().value();
        String key = method + ":" + path;

        MockResponse mock = mocks.get(key);
        if (mock == null) {
            return chain.filter(exchange);
        }

        // Simulate delay
        Mono<Void> response;
        if (mock.delayMs() > 0) {
            response = Mono.delay(java.time.Duration.ofMillis(mock.delayMs()))
                    .then(writeMockResponse(exchange, mock));
        } else {
            response = writeMockResponse(exchange, mock);
        }

        return response;
    }

    private Mono<Void> writeMockResponse(ServerWebExchange exchange, MockResponse mock) {
        exchange.getResponse().setStatusCode(HttpStatus.valueOf(mock.statusCode()));
        exchange.getResponse().getHeaders().setContentType(
                MediaType.parseMediaType(mock.contentType()));
        exchange.getResponse().getHeaders().set("X-Mock", "true");
        DataBuffer buffer = exchange.getResponse().bufferFactory()
                .wrap(mock.body().getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    public int getMockCount() {
        return mocks.size();
    }

    @Override
    public int getOrder() {
        return 0; // Very first - before WAF, since mocks don't need security
    }

    public record MockResponse(int statusCode, String contentType, String body, int delayMs) {}
}
