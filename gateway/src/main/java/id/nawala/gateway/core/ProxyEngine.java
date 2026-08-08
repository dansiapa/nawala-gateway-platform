package id.nawala.gateway.core;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProxyEngine {

    private final WebClient.Builder webClientBuilder;
    private final Map<String, WebClient> clientCache = new ConcurrentHashMap<>();

    public Mono<ResponseEntity<byte[]>> proxy(ProxyRequest request) {
        log.debug("Proxying {} {} -> {}", request.getMethod(), request.getPath(), request.getTargetUrl());
        
        WebClient client = getOrCreateClient(request.getTargetUrl());
        String targetPath = buildTargetPath(request);
        
        WebClient.RequestBodySpec requestSpec = client
                .method(HttpMethod.valueOf(request.getMethod()))
                .uri(targetPath);
        
        // Copy headers
        if (request.getHeaders() != null) {
            request.getHeaders().forEach((key, values) -> {
                if (!isHopByHopHeader(key)) {
                    values.forEach(v -> requestSpec.header(key, v));
                }
            });
        }
        
        // Add gateway headers
        requestSpec.header("X-Forwarded-For", request.getClientIp());
        requestSpec.header("X-Forwarded-Host", request.getOriginalHost());
        requestSpec.header("X-Gateway-Request-Id", request.getRequestId());
        
        // Handle body
        Mono<ResponseEntity<byte[]>> responseMono;
        if (request.getBody() != null && request.getBody().length > 0) {
            responseMono = requestSpec
                    .bodyValue(request.getBody())
                    .retrieve()
                    .toEntity(byte[].class);
        } else {
            responseMono = requestSpec
                    .retrieve()
                    .toEntity(byte[].class);
        }
        
        return responseMono
                .timeout(Duration.ofSeconds(request.getTimeoutSeconds() > 0 ? request.getTimeoutSeconds() : 30))
                .map(response -> transformResponse(response, request))
                .onErrorResume(WebClientResponseException.class, e -> {
                    log.warn("Upstream error: {} {}", e.getStatusCode(), e.getMessage());
                    return Mono.just(ResponseEntity
                            .status(e.getStatusCode())
                            .headers(filterResponseHeaders(e.getHeaders()))
                            .body(e.getResponseBodyAsByteArray()));
                })
                .onErrorResume(Exception.class, e -> {
                    log.error("Proxy error: {}", e.getMessage());
                    return Mono.just(ResponseEntity
                            .status(HttpStatus.BAD_GATEWAY)
                            .body(("Gateway error: " + e.getMessage()).getBytes()));
                });
    }
    
    private WebClient getOrCreateClient(String baseUrl) {
        String host = extractHost(baseUrl);
        return clientCache.computeIfAbsent(host, h -> 
            webClientBuilder
                .baseUrl(extractBaseUrl(baseUrl))
                .build()
        );
    }
    
    private String extractHost(String url) {
        try {
            java.net.URI uri = new java.net.URI(url);
            return uri.getHost() + ":" + (uri.getPort() > 0 ? uri.getPort() : 80);
        } catch (Exception e) {
            return url;
        }
    }
    
    private String extractBaseUrl(String url) {
        try {
            java.net.URI uri = new java.net.URI(url);
            return uri.getScheme() + "://" + uri.getHost() + (uri.getPort() > 0 ? ":" + uri.getPort() : "");
        } catch (Exception e) {
            return url;
        }
    }
    
    private String buildTargetPath(ProxyRequest request) {
        String targetUrl = request.getTargetUrl();
        try {
            java.net.URI uri = new java.net.URI(targetUrl);
            String path = uri.getPath();
            if (request.getQueryString() != null && !request.getQueryString().isEmpty()) {
                path += "?" + request.getQueryString();
            }
            return path;
        } catch (Exception e) {
            return targetUrl;
        }
    }
    
    private ResponseEntity<byte[]> transformResponse(ResponseEntity<byte[]> response, ProxyRequest request) {
        HttpHeaders headers = filterResponseHeaders(response.getHeaders());
        headers.add("X-Gateway-Response-Time", String.valueOf(System.currentTimeMillis() - request.getStartTime()));
        headers.add("X-Gateway-Request-Id", request.getRequestId());
        
        return ResponseEntity
                .status(response.getStatusCode())
                .headers(headers)
                .body(response.getBody());
    }
    
    private HttpHeaders filterResponseHeaders(HttpHeaders original) {
        HttpHeaders filtered = new HttpHeaders();
        original.forEach((key, values) -> {
            if (!isHopByHopHeader(key)) {
                filtered.addAll(key, values);
            }
        });
        return filtered;
    }
    
    private boolean isHopByHopHeader(String header) {
        String lower = header.toLowerCase();
        return lower.equals("connection") ||
               lower.equals("keep-alive") ||
               lower.equals("proxy-authenticate") ||
               lower.equals("proxy-authorization") ||
               lower.equals("te") ||
               lower.equals("trailer") ||
               lower.equals("transfer-encoding") ||
               lower.equals("upgrade") ||
               lower.equals("host");
    }
}
