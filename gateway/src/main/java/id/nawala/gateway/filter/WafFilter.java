package id.nawala.gateway.filter;

import id.nawala.gateway.logging.SecurityLogger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * WAF (Web Application Firewall) filter for the gateway.
 * Inspects requests for SQL injection, XSS, and path traversal.
 */
@Component
@Slf4j
public class WafFilter implements GlobalFilter, Ordered {

    private static final Pattern SQL_INJECTION = Pattern.compile(
            "(?i)(union\\s+select|drop\\s+table|insert\\s+into|delete\\s+from|" +
            "or\\s+1\\s*=\\s*1|'\\s*or\\s*'|;\\s*--)", Pattern.CASE_INSENSITIVE);

    private static final Pattern XSS = Pattern.compile(
            "(?i)(<script|javascript:|on\\w+\\s*=|<iframe|<object|alert\\s*\\(|" +
            "document\\.cookie|eval\\s*\\()", Pattern.CASE_INSENSITIVE);

    private static final Pattern PATH_TRAVERSAL = Pattern.compile(
            "(\\.\\./|\\.\\.\\\\|%2e%2e|%252e%252e)", Pattern.CASE_INSENSITIVE);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();
        String query = request.getURI().getRawQuery();
        String fullInput = path + (query != null ? "?" + query : "");

        // Check path + query string
        String blocked = inspect(fullInput);
        if (blocked != null) {
            String clientIp = request.getRemoteAddress() != null
                    ? request.getRemoteAddress().getAddress().getHostAddress() : "unknown";
            SecurityLogger.log().warn("WAF BLOCKED type={} ip={} path={}", blocked, clientIp, path);
            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
            exchange.getResponse().getHeaders().set("X-WAF-Block", blocked);
            DataBuffer buffer = exchange.getResponse().bufferFactory()
                    .wrap("{\"error\":\"Blocked by WAF\",\"reason\":\"" + blocked + "\"}".getBytes(StandardCharsets.UTF_8));
            return exchange.getResponse().writeWith(Mono.just(buffer));
        }

        return chain.filter(exchange);
    }

    private String inspect(String input) {
        if (SQL_INJECTION.matcher(input).find()) return "SQL_INJECTION";
        if (XSS.matcher(input).find()) return "XSS";
        if (PATH_TRAVERSAL.matcher(input).find()) return "PATH_TRAVERSAL";
        return null;
    }

    @Override
    public int getOrder() {
        return 1; // Very early, right after access log
    }
}
