package id.nawala.platform.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * API Analytics service for traffic intelligence and reporting.
 */
public interface AnalyticsService {

    void recordRequest(Long routeId, String apiKeyPrefix, String sourceIp,
                       String method, String path, int statusCode,
                       long responseTimeMs, long requestSize, long responseSize);

    AnalyticsSummary getSummary(LocalDateTime since);

    List<RouteAnalytics> getTopRoutes(LocalDateTime since, int limit);

    List<KeyAnalytics> getTopKeys(LocalDateTime since, int limit);

    Map<Integer, Long> getStatusDistribution(LocalDateTime since);

    Map<Integer, Long> getHourlyTraffic(LocalDateTime since);

    Map<String, Long> getGeoDistribution(LocalDateTime since);

    record AnalyticsSummary(
            long totalRequests,
            double avgResponseTimeMs,
            long errorCount,
            double errorRate,
            long uniqueIps
    ) {}

    record RouteAnalytics(
            String path,
            long requestCount,
            double avgResponseTime
    ) {}

    record KeyAnalytics(
            String keyPrefix,
            long requestCount
    ) {}
}
