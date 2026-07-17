package id.nawala.platform.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Tracks per-route analytics: request count, latency, errors.
 * Aggregated per minute for real-time dashboard.
 */
@Entity
@Table(name = "api_analytics", indexes = {
    @Index(name = "idx_analytics_route_time", columnList = "routeId,recordedAt"),
    @Index(name = "idx_analytics_api_key", columnList = "apiKeyPrefix,recordedAt")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiAnalytics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long routeId;
    private String apiKeyPrefix;
    private String sourceIp;
    private String method;
    private String path;

    private int statusCode;
    private long responseTimeMs;
    private long requestSizeBytes;
    private long responseSizeBytes;

    private String country;
    private String city;

    private LocalDateTime recordedAt;

    @PrePersist
    protected void onCreate() {
        if (recordedAt == null) recordedAt = LocalDateTime.now();
    }
}
