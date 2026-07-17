package id.nawala.platform.service;

import id.nawala.platform.model.ApiRoute;

import java.util.List;
import java.util.Map;

/**
 * Health monitoring service for registered API routes.
 */
public interface HealthMonitorService {

    /**
     * Run health checks on all routes that have healthCheckUrl configured.
     */
    void checkAllRoutes();

    /**
     * Get health summary for dashboard.
     */
    HealthSummary getHealthSummary();

    /**
     * Get detailed health status per route.
     */
    List<RouteHealth> getAllRouteHealth();

    record HealthSummary(
            int totalMonitored,
            int upCount,
            int downCount,
            int degradedCount,
            int unknownCount
    ) {}

    record RouteHealth(
            Long routeId,
            String name,
            String path,
            String status,
            Integer responseTimeMs,
            String lastChecked
    ) {}
}
