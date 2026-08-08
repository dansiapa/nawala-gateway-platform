package id.nawala.gateway.graphql;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import lombok.extern.slf4j.Slf4j;

/**
 * GraphQL Proxy Filter - Routes GraphQL requests to appropriate backend
 */
@Slf4j
@Component
public class GraphQLProxyFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        String contentType = exchange.getRequest().getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
        
        // Check if GraphQL request
        if (isGraphQLRequest(path, contentType)) {
            log.debug("GraphQL request detected: {}", path);
            exchange.getAttributes().put("isGraphQL", true);
            
            // Add GraphQL-specific headers
            exchange.getRequest().mutate()
                .header("X-GraphQL-Gateway", "nawala")
                .build();
        }
        
        return chain.filter(exchange);
    }

    private boolean isGraphQLRequest(String path, String contentType) {
        return path.contains("/graphql") || 
               (contentType != null && contentType.contains("application/graphql"));
    }

    @Override
    public int getOrder() {
        return -5;
    }
}
