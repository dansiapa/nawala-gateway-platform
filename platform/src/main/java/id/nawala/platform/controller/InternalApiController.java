package id.nawala.platform.controller;

import id.nawala.platform.model.ApiRoute;
import id.nawala.platform.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Internal API endpoints called by the Gateway service.
 * These are not exposed to end users - secured via network isolation.
 */
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalApiController {

    private final ApiRouteService apiRouteService;
    private final ApiKeyService apiKeyService;
    private final AnomalyDetectionService anomalyDetectionService;
    private final HealthMonitorService healthMonitorService;
    private final OAuthService oAuthService;
    private final AnalyticsService analyticsService;
    private final WafService wafService;
    private final PluginService pluginService;

    @GetMapping("/routes")
    public ResponseEntity<List<Map<String, Object>>> getActiveRoutes() {
        List<Map<String, Object>> routes = apiRouteService.findActiveRoutes().stream()
                .map(this::routeToMap)
                .collect(Collectors.toList());
        return ResponseEntity.ok(routes);
    }

    @PostMapping("/keys/validate")
    public ResponseEntity<Map<String, Object>> validateKey(@RequestBody Map<String, Object> request) {
        String rawKey = (String) request.get("key");
        if (rawKey == null || rawKey.isBlank()) {
            return ResponseEntity.ok(Map.of("valid", false, "message", "Key is required"));
        }
        String ip = (String) request.getOrDefault("ip", "");
        String method = (String) request.getOrDefault("method", "");
        Number routeIdNum = (Number) request.get("routeId");
        Long routeId = routeIdNum != null ? routeIdNum.longValue() : null;

        boolean valid;
        if (ip != null && !ip.isBlank()) {
            valid = apiKeyService.validateWithScope(rawKey, ip, method, routeId);
        } else {
            valid = apiKeyService.validate(rawKey);
        }
        Map<String, Object> response = new HashMap<>();
        response.put("valid", valid);
        if (valid) {
            String prefix = rawKey.substring(0, 8);
            apiKeyService.incrementUsage(prefix);
            response.put("prefix", prefix);
            response.put("message", "Key is valid");
        } else {
            response.put("message", "Invalid, expired, or scope-restricted key");
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/oauth/validate")
    public ResponseEntity<Map<String, Object>> validateOAuthToken(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        if (token == null || token.isBlank()) {
            return ResponseEntity.ok(Map.of("valid", false));
        }
        OAuthService.TokenInfo info = oAuthService.validateToken(token);
        if (info == null) {
            return ResponseEntity.ok(Map.of("valid", false));
        }
        Map<String, Object> resp = new HashMap<>();
        resp.put("valid", true);
        resp.put("clientId", info.clientId());
        resp.put("scopes", info.scopes());
        resp.put("expiresIn", info.expiresInSeconds());
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/analytics/record")
    public ResponseEntity<Map<String, Object>> recordAnalytics(@RequestBody Map<String, Object> request) {
        Number routeIdNum = (Number) request.get("routeId");
        Long routeId = routeIdNum != null ? routeIdNum.longValue() : null;
        String apiKeyPrefix = (String) request.get("apiKeyPrefix");
        String sourceIp = (String) request.get("sourceIp");
        String method = (String) request.get("method");
        String path = (String) request.get("path");
        int statusCode = request.get("statusCode") != null ? ((Number) request.get("statusCode")).intValue() : 200;
        long responseTimeMs = request.get("responseTimeMs") != null ? ((Number) request.get("responseTimeMs")).longValue() : 0;
        long requestSize = request.get("requestSize") != null ? ((Number) request.get("requestSize")).longValue() : 0;
        long responseSize = request.get("responseSize") != null ? ((Number) request.get("responseSize")).longValue() : 0;
        analyticsService.recordRequest(routeId, apiKeyPrefix, sourceIp, method, path, statusCode, responseTimeMs, requestSize, responseSize);
        return ResponseEntity.ok(Map.of("recorded", true));
    }

    @GetMapping("/waf/rules")
    public ResponseEntity<?> getWafRules() {
        return ResponseEntity.ok(wafService.getAllActiveRules());
    }

    @GetMapping("/plugins/{hookType}")
    public ResponseEntity<?> getPluginsByHook(@PathVariable String hookType, @RequestParam(required = false) Long routeId) {
        return ResponseEntity.ok(pluginService.getActiveByHook(hookType, routeId));
    }

    @PostMapping("/anomaly/record")
    public ResponseEntity<Map<String, Object>> recordRequest(@RequestBody Map<String, Object> request) {
        String sourceIp = (String) request.get("sourceIp");
        String apiKeyPrefix = (String) request.get("apiKeyPrefix");
        String path = (String) request.get("path");
        int status = request.get("status") != null ? ((Number) request.get("status")).intValue() : 200;
        anomalyDetectionService.recordRequest(sourceIp, apiKeyPrefix, path, status);
        return ResponseEntity.ok(Map.of("recorded", true));
    }

    @PostMapping("/anomaly/check-block")
    public ResponseEntity<Map<String, Object>> checkBlock(@RequestBody Map<String, String> request) {
        String sourceIp = request.get("sourceIp");
        String apiKeyPrefix = request.get("apiKeyPrefix");
        boolean blocked = anomalyDetectionService.isBlocked(sourceIp, apiKeyPrefix);
        Map<String, Object> response = new HashMap<>();
        response.put("blocked", blocked);
        if (blocked) response.put("message", "Source is temporarily blocked");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health/summary")
    public ResponseEntity<HealthMonitorService.HealthSummary> getHealthSummary() {
        return ResponseEntity.ok(healthMonitorService.getHealthSummary());
    }

    @GetMapping("/health/routes")
    public ResponseEntity<List<HealthMonitorService.RouteHealth>> getRouteHealth() {
        return ResponseEntity.ok(healthMonitorService.getAllRouteHealth());
    }

    private Map<String, Object> routeToMap(ApiRoute route) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", route.getId());
        map.put("name", route.getName());
        map.put("method", route.getMethod());
        map.put("path", route.getPath());
        map.put("maskedPath", route.getMaskedPath());
        map.put("targetUrl", route.getTargetUrl());
        map.put("authRequired", route.isAuthRequired());
        map.put("rateLimitEnabled", route.isRateLimitEnabled());
        map.put("rateLimitPerMinute", route.getRateLimitPerMinute());
        map.put("payloadEncryption", route.isPayloadEncryption());
        map.put("healthStatus", route.getHealthStatus());
        map.put("active", route.isActive());
        return map;
    }
}
