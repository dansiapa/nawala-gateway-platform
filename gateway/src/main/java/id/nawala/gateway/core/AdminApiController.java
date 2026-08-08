package id.nawala.gateway.core;

import id.nawala.gateway.circuitbreaker.CircuitBreaker;
import id.nawala.gateway.filter.RateLimiter;
import id.nawala.gateway.filter.ResponseCache;
import id.nawala.gateway.filter.WafFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/admin/api")
@RequiredArgsConstructor
public class AdminApiController {
    
    private final RouteResolver routeResolver;
    private final ServiceDiscovery serviceDiscovery;
    private final CircuitBreaker circuitBreaker;
    private final ResponseCache responseCache;
    private final WafFilter wafFilter;
    private final AnalyticsCollector analyticsCollector;
    private final RequestLogger requestLogger;
    
    // Routes Management
    @PostMapping("/routes/refresh")
    public Mono<ResponseEntity<Map<String, String>>> refreshRoutes() {
        routeResolver.refreshRoutes();
        return Mono.just(ResponseEntity.ok(Map.of("status", "refreshed")));
    }
    
    // Service Discovery
    @GetMapping("/services")
    public Mono<ResponseEntity<Set<String>>> listServices() {
        return Mono.just(ResponseEntity.ok(serviceDiscovery.getServiceNames()));
    }
    
    @GetMapping("/services/{name}")
    public Mono<ResponseEntity<?>> getService(@PathVariable String name) {
        return Mono.just(ResponseEntity.ok(serviceDiscovery.getAllInstances(name)));
    }
    
    @PostMapping("/services/register")
    public Mono<ResponseEntity<Map<String, String>>> registerService(
            @RequestParam String name,
            @RequestParam String url,
            @RequestParam(required = false) String metadata) {
        serviceDiscovery.register(name, url, metadata);
        return Mono.just(ResponseEntity.ok(Map.of("status", "registered")));
    }
    
    @PostMapping("/services/deregister")
    public Mono<ResponseEntity<Map<String, String>>> deregisterService(
            @RequestParam String name,
            @RequestParam String url) {
        serviceDiscovery.deregister(name, url);
        return Mono.just(ResponseEntity.ok(Map.of("status", "deregistered")));
    }
    
    @PostMapping("/services/heartbeat")
    public Mono<ResponseEntity<Map<String, String>>> heartbeat(
            @RequestParam String name,
            @RequestParam String url) {
        serviceDiscovery.heartbeat(name, url);
        return Mono.just(ResponseEntity.ok(Map.of("status", "ok")));
    }
    
    // Circuit Breaker
    @GetMapping("/circuit-breaker/{target}")
    public Mono<ResponseEntity<Map<String, String>>> getCircuitStatus(@PathVariable String target) {
        return Mono.just(ResponseEntity.ok(Map.of(
            "target", target,
            "status", circuitBreaker.getStatus(target).name()
        )));
    }
    
    @PostMapping("/circuit-breaker/{target}/reset")
    public Mono<ResponseEntity<Map<String, String>>> resetCircuit(@PathVariable String target) {
        circuitBreaker.reset(target);
        return Mono.just(ResponseEntity.ok(Map.of("status", "reset")));
    }
    
    // Cache Management
    @GetMapping("/cache/stats")
    public Mono<ResponseEntity<Map<String, Object>>> getCacheStats() {
        return Mono.just(ResponseEntity.ok(Map.of(
            "size", responseCache.size()
        )));
    }
    
    @PostMapping("/cache/clear")
    public Mono<ResponseEntity<Map<String, String>>> clearCache() {
        responseCache.clear();
        return Mono.just(ResponseEntity.ok(Map.of("status", "cleared")));
    }
    
    @DeleteMapping("/cache/{key}")
    public Mono<ResponseEntity<Map<String, String>>> invalidateCache(@PathVariable String key) {
        responseCache.invalidate(key);
        return Mono.just(ResponseEntity.ok(Map.of("status", "invalidated")));
    }
    
    // WAF Management
    @GetMapping("/waf/blocked-ips")
    public Mono<ResponseEntity<Set<String>>> getBlockedIps() {
        return Mono.just(ResponseEntity.ok(wafFilter.getBlockedIps()));
    }
    
    @PostMapping("/waf/block-ip")
    public Mono<ResponseEntity<Map<String, String>>> blockIp(@RequestParam String ip) {
        wafFilter.blockIp(ip);
        return Mono.just(ResponseEntity.ok(Map.of("status", "blocked")));
    }
    
    @PostMapping("/waf/unblock-ip")
    public Mono<ResponseEntity<Map<String, String>>> unblockIp(@RequestParam String ip) {
        wafFilter.unblockIp(ip);
        return Mono.just(ResponseEntity.ok(Map.of("status", "unblocked")));
    }
    
    @PostMapping("/waf/feature")
    public Mono<ResponseEntity<Map<String, String>>> setWafFeature(
            @RequestParam String feature,
            @RequestParam boolean enabled) {
        wafFilter.setFeatureEnabled(feature, enabled);
        return Mono.just(ResponseEntity.ok(Map.of("status", "updated")));
    }
    
    // Analytics
    @GetMapping("/stats")
    public Mono<ResponseEntity<AnalyticsCollector.Stats>> getStats() {
        return Mono.just(ResponseEntity.ok(analyticsCollector.getRealtimeStats()));
    }
    
    // Logging
    @PostMapping("/logging/detailed")
    public Mono<ResponseEntity<Map<String, String>>> setDetailedLogging(@RequestParam boolean enabled) {
        requestLogger.setDetailedLogging(enabled);
        return Mono.just(ResponseEntity.ok(Map.of("status", enabled ? "enabled" : "disabled")));
    }
}
