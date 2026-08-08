package id.nawala.gateway.core;

import id.nawala.gateway.filter.AuthFilter;
import id.nawala.gateway.filter.RateLimiter;
import id.nawala.gateway.filter.WafFilter;
import id.nawala.gateway.loadbalancer.LoadBalancer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
public class GatewayController {
    
    private final ProxyEngine proxyEngine;
    private final RouteResolver routeResolver;
    private final LoadBalancer loadBalancer;
    private final RateLimiter rateLimiter;
    private final AuthFilter authFilter;
    private final WafFilter wafFilter;
    
    @RequestMapping(value = "/gw/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, 
            RequestMethod.DELETE, RequestMethod.PATCH, RequestMethod.OPTIONS, RequestMethod.HEAD})
    public Mono<ResponseEntity<byte[]>> handleRequest(ServerHttpRequest request, @RequestBody(required = false) byte[] body) {
        long startTime = System.currentTimeMillis();
        String requestId = UUID.randomUUID().toString();
        String path = request.getPath().value().replaceFirst("^/gw", "");
        String method = request.getMethod().name();
        String clientIp = getClientIp(request);
        
        log.debug("[{}] {} {} from {}", requestId, method, path, clientIp);
        
        // 1. WAF Check
        WafFilter.WafResult wafResult = wafFilter.check(
            clientIp, path, request.getURI().getQuery(), 
            body != null ? new String(body) : null, null
        );
        if (!wafResult.allowed()) {
            log.warn("[{}] WAF blocked: {}", requestId, wafResult.code());
            return Mono.just(errorResponse(HttpStatus.FORBIDDEN, wafResult.message(), requestId));
        }
        
        // 2. Route Resolution
        Optional<RouteDefinition> routeOpt = routeResolver.resolve(method, path);
        if (routeOpt.isEmpty()) {
            log.debug("[{}] No route found for {} {}", requestId, method, path);
            return Mono.just(errorResponse(HttpStatus.NOT_FOUND, "No route found", requestId));
        }
        
        RouteDefinition route = routeOpt.get();
        String clientKey = clientIp;
        
        // 3. Authentication
        if (route.isAuthRequired()) {
            String apiKey = extractApiKey(request);
            String bearerToken = extractBearerToken(request);
            
            if (apiKey != null) {
                Optional<AuthFilter.ApiKeyInfo> keyInfo = authFilter.validateApiKey(apiKey);
                if (keyInfo.isEmpty()) {
                    return Mono.just(errorResponse(HttpStatus.UNAUTHORIZED, "Invalid API key", requestId));
                }
                clientKey = "key:" + keyInfo.get().keyId();
            } else if (bearerToken != null) {
                Optional<AuthFilter.OAuthTokenInfo> tokenInfo = authFilter.validateOAuthToken(bearerToken);
                if (tokenInfo.isEmpty()) {
                    return Mono.just(errorResponse(HttpStatus.UNAUTHORIZED, "Invalid token", requestId));
                }
                clientKey = "oauth:" + tokenInfo.get().clientId();
            } else {
                return Mono.just(errorResponse(HttpStatus.UNAUTHORIZED, "Authentication required", requestId));
            }
        }
        
        // 4. Rate Limiting
        if (route.isRateLimitEnabled()) {
            int limit = route.getRateLimitPerMinute();
            if (!rateLimiter.tryAcquire(clientKey, route.getId(), limit)) {
                RateLimiter.RateLimitInfo info = rateLimiter.getRateLimitInfo(clientKey, route.getId(), limit);
                HttpHeaders headers = new HttpHeaders();
                headers.add("X-RateLimit-Limit", String.valueOf(info.limit()));
                headers.add("X-RateLimit-Remaining", "0");
                headers.add("X-RateLimit-Reset", String.valueOf(info.resetIn()));
                headers.add("Retry-After", String.valueOf(info.resetIn()));
                
                return Mono.just(ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .headers(headers)
                        .body("{\"error\":\"Rate limit exceeded\"}".getBytes()));
            }
        }
        
        // 5. Load Balancing
        String targetUrl = loadBalancer.selectTarget(route);
        
        // 6. Build proxy request
        ProxyRequest proxyRequest = ProxyRequest.builder()
                .requestId(requestId)
                .method(method)
                .path(path)
                .queryString(request.getURI().getQuery())
                .targetUrl(targetUrl)
                .headers(request.getHeaders())
                .body(body)
                .clientIp(clientIp)
                .originalHost(request.getHeaders().getHost() != null ? request.getHeaders().getHost().toString() : "")
                .timeoutSeconds(route.getTimeoutSeconds() > 0 ? route.getTimeoutSeconds() : 30)
                .startTime(startTime)
                .routeId(route.getId())
                .routeName(route.getName())
                .build();
        
        // 7. Proxy the request
        return proxyEngine.proxy(proxyRequest)
                .doOnSuccess(response -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.info("[{}] {} {} -> {} {}ms", requestId, method, path, 
                            response.getStatusCode().value(), duration);
                })
                .doOnError(error -> {
                    log.error("[{}] Proxy error: {}", requestId, error.getMessage());
                });
    }
    
    private String getClientIp(ServerHttpRequest request) {
        String xff = request.getHeaders().getFirst("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        String xri = request.getHeaders().getFirst("X-Real-IP");
        if (xri != null && !xri.isBlank()) {
            return xri;
        }
        return request.getRemoteAddress() != null ? 
               request.getRemoteAddress().getAddress().getHostAddress() : "unknown";
    }
    
    private String extractApiKey(ServerHttpRequest request) {
        String header = request.getHeaders().getFirst("X-API-Key");
        if (header != null) return header;
        
        String query = request.getURI().getQuery();
        if (query != null && query.contains("api_key=")) {
            for (String param : query.split("&")) {
                if (param.startsWith("api_key=")) {
                    return param.substring(8);
                }
            }
        }
        return null;
    }
    
    private String extractBearerToken(ServerHttpRequest request) {
        String auth = request.getHeaders().getFirst("Authorization");
        if (auth != null && auth.toLowerCase().startsWith("bearer ")) {
            return auth.substring(7);
        }
        return null;
    }
    
    private ResponseEntity<byte[]> errorResponse(HttpStatus status, String message, String requestId) {
        String json = String.format("{\"error\":\"%s\",\"requestId\":\"%s\"}", message, requestId);
        return ResponseEntity.status(status)
                .header("Content-Type", "application/json")
                .header("X-Gateway-Request-Id", requestId)
                .body(json.getBytes());
    }
    
    // Health check endpoint
    @GetMapping("/health")
    public Mono<ResponseEntity<String>> health() {
        return Mono.just(ResponseEntity.ok("{\"status\":\"UP\"}"));
    }
    
    // Route refresh endpoint (admin only)
    @PostMapping("/admin/routes/refresh")
    public Mono<ResponseEntity<String>> refreshRoutes() {
        routeResolver.refreshRoutes();
        return Mono.just(ResponseEntity.ok("{\"status\":\"refreshed\"}"));
    }
}
