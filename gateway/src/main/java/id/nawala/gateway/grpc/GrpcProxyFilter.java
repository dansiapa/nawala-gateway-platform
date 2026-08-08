package id.nawala.gateway.grpc;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import lombok.extern.slf4j.Slf4j;

/**
 * gRPC-Web Proxy Filter - Translates gRPC-Web to gRPC and vice versa
 */
@Slf4j
@Component
public class GrpcProxyFilter implements GlobalFilter, Ordered {

    private static final String GRPC_WEB_CONTENT_TYPE = "application/grpc-web";
    private static final String GRPC_CONTENT_TYPE = "application/grpc";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String contentType = exchange.getRequest().getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
        
        if (isGrpcWebRequest(contentType)) {
            log.debug("gRPC-Web request detected");
            exchange.getAttributes().put("isGrpcWeb", true);
            
            // Transform gRPC-Web to gRPC
            return handleGrpcWebRequest(exchange, chain);
        }
        
        return chain.filter(exchange);
    }

    private boolean isGrpcWebRequest(String contentType) {
        return contentType != null && 
               (contentType.startsWith(GRPC_WEB_CONTENT_TYPE) || 
                contentType.startsWith(GRPC_CONTENT_TYPE));
    }

    private Mono<Void> handleGrpcWebRequest(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Add gRPC headers
        exchange.getRequest().mutate()
            .header("X-GRPC-Gateway", "nawala")
            .header("grpc-accept-encoding", "identity")
            .build();
        
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -4;
    }
}
