package id.nawala.gateway.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class GraphQLProxy {
    
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;
    
    public Mono<ResponseEntity<byte[]>> proxy(GraphQLRequest request) {
        log.debug("Proxying GraphQL to {}", request.targetUrl());
        
        return webClientBuilder.build()
            .post()
            .uri(request.targetUrl())
            .contentType(MediaType.APPLICATION_JSON)
            .headers(h -> copyHeaders(request.headers(), h))
            .bodyValue(request.body())
            .retrieve()
            .toEntity(byte[].class)
            .map(response -> {
                // Add GraphQL-specific headers
                HttpHeaders headers = new HttpHeaders();
                headers.addAll(response.getHeaders());
                headers.add("X-GraphQL-Gateway", "nawala");
                
                return ResponseEntity
                    .status(response.getStatusCode())
                    .headers(headers)
                    .body(response.getBody());
            })
            .onErrorResume(e -> {
                log.error("GraphQL proxy error: {}", e.getMessage());
                String error = String.format(
                    "{\"errors\":[{\"message\":\"Gateway error: %s\"}]}", 
                    e.getMessage().replace("\"", "\\\"")
                );
                return Mono.just(ResponseEntity
                    .status(502)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(error.getBytes()));
            });
    }
    
    public boolean isGraphQLRequest(String contentType, byte[] body) {
        if (contentType != null && contentType.contains("application/graphql")) {
            return true;
        }
        
        if (body != null && body.length > 0) {
            try {
                JsonNode node = objectMapper.readTree(body);
                return node.has("query") || node.has("mutation");
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }
    
    public GraphQLOperation parseOperation(byte[] body) {
        try {
            JsonNode node = objectMapper.readTree(body);
            String query = node.has("query") ? node.get("query").asText() : null;
            String operationName = node.has("operationName") ? node.get("operationName").asText() : null;
            JsonNode variables = node.has("variables") ? node.get("variables") : null;
            
            OperationType type = OperationType.QUERY;
            if (query != null) {
                String trimmed = query.trim().toLowerCase();
                if (trimmed.startsWith("mutation")) {
                    type = OperationType.MUTATION;
                } else if (trimmed.startsWith("subscription")) {
                    type = OperationType.SUBSCRIPTION;
                }
            }
            
            return new GraphQLOperation(query, operationName, variables, type);
        } catch (Exception e) {
            log.error("Failed to parse GraphQL operation: {}", e.getMessage());
            return null;
        }
    }
    
    private void copyHeaders(HttpHeaders source, HttpHeaders target) {
        if (source != null) {
            source.forEach((key, values) -> {
                if (!key.equalsIgnoreCase("host") && 
                    !key.equalsIgnoreCase("content-length")) {
                    target.addAll(key, values);
                }
            });
        }
    }
    
    public record GraphQLRequest(
        String targetUrl,
        byte[] body,
        HttpHeaders headers,
        String clientIp
    ) {}
    
    public record GraphQLOperation(
        String query,
        String operationName,
        JsonNode variables,
        OperationType type
    ) {}
    
    public enum OperationType {
        QUERY, MUTATION, SUBSCRIPTION
    }
}
