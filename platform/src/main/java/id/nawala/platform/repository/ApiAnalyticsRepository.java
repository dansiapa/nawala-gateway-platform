package id.nawala.platform.repository;

import id.nawala.platform.model.ApiAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ApiAnalyticsRepository extends JpaRepository<ApiAnalytics, Long> {

    @Query("SELECT COUNT(a) FROM ApiAnalytics a WHERE a.recordedAt >= :since")
    long countSince(@Param("since") LocalDateTime since);

    @Query("SELECT AVG(a.responseTimeMs) FROM ApiAnalytics a WHERE a.recordedAt >= :since")
    Double avgResponseTimeSince(@Param("since") LocalDateTime since);

    @Query("SELECT COUNT(a) FROM ApiAnalytics a WHERE a.statusCode >= 500 AND a.recordedAt >= :since")
    long countErrorsSince(@Param("since") LocalDateTime since);

    @Query("SELECT a.path, COUNT(a), AVG(a.responseTimeMs) FROM ApiAnalytics a WHERE a.recordedAt >= :since GROUP BY a.path ORDER BY COUNT(a) DESC")
    List<Object[]> getTopRoutesSince(@Param("since") LocalDateTime since);

    @Query("SELECT a.apiKeyPrefix, COUNT(a) FROM ApiAnalytics a WHERE a.recordedAt >= :since AND a.apiKeyPrefix IS NOT NULL GROUP BY a.apiKeyPrefix ORDER BY COUNT(a) DESC")
    List<Object[]> getTopKeysSince(@Param("since") LocalDateTime since);

    @Query("SELECT a.statusCode, COUNT(a) FROM ApiAnalytics a WHERE a.recordedAt >= :since GROUP BY a.statusCode ORDER BY a.statusCode")
    List<Object[]> getStatusDistribution(@Param("since") LocalDateTime since);

    @Query("SELECT HOUR(a.recordedAt), COUNT(a) FROM ApiAnalytics a WHERE a.recordedAt >= :since GROUP BY HOUR(a.recordedAt) ORDER BY HOUR(a.recordedAt)")
    List<Object[]> getHourlyTraffic(@Param("since") LocalDateTime since);

    @Query("SELECT a.country, COUNT(a) FROM ApiAnalytics a WHERE a.recordedAt >= :since AND a.country IS NOT NULL GROUP BY a.country ORDER BY COUNT(a) DESC")
    List<Object[]> getGeoDistribution(@Param("since") LocalDateTime since);

    List<ApiAnalytics> findByRouteIdAndRecordedAtAfter(Long routeId, LocalDateTime since);
}
