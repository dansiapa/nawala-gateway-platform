package id.nawala.platform.service;

import id.nawala.platform.model.AnomalyEvent;

import java.util.List;

/**
 * Anomaly detection and threat management service.
 */
public interface AnomalyDetectionService {

    /**
     * Record an API request for pattern analysis.
     */
    void recordRequest(String sourceIp, String apiKeyPrefix, String path, int responseStatus);

    /**
     * Check if a source IP or key is currently blocked.
     */
    boolean isBlocked(String sourceIp, String apiKeyPrefix);

    /**
     * Get all unresolved anomaly events.
     */
    List<AnomalyEvent> getUnresolvedEvents();

    /**
     * Get recent anomaly events (last 20).
     */
    List<AnomalyEvent> getRecentEvents();

    /**
     * Resolve an anomaly event (admin action).
     */
    void resolveEvent(Long eventId, Long resolvedByUserId);

    /**
     * Get threat stats for dashboard.
     */
    ThreatStats getThreatStats();

    record ThreatStats(
            long unresolvedCount,
            long criticalCount,
            long blockedCount,
            long last24hCount
    ) {}
}
