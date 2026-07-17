package id.nawala.platform.model;

import id.nawala.platform.util.FieldEncryptor;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Tracks anomalous API usage patterns for threat detection.
 */
@Entity
@Table(name = "anomaly_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnomalyEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String type; // SPIKE, BRUTE_FORCE, GEO_ANOMALY, UNUSUAL_HOUR, REPEATED_FAILURE

    @Column(nullable = false, length = 50)
    private String severity; // LOW, MEDIUM, HIGH, CRITICAL

    @Convert(converter = FieldEncryptor.class)
    @Column(length = 500)
    private String sourceIp;

    @Column(length = 100)
    private String apiKeyPrefix;

    @Convert(converter = FieldEncryptor.class)
    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private int requestCount;

    @Column(length = 500)
    private String targetPath;

    @Column(nullable = false)
    private boolean resolved;

    @Column(nullable = false)
    private boolean autoBlocked;

    private LocalDateTime blockedUntil;

    @Column(nullable = false, updatable = false)
    private LocalDateTime detectedAt;

    private LocalDateTime resolvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by")
    private User resolvedBy;

    @PrePersist
    protected void onCreate() {
        detectedAt = LocalDateTime.now();
    }
}
