package id.nawala.platform.model;

import id.nawala.platform.util.FieldEncryptor;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "api_routes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiRoute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Convert(converter = FieldEncryptor.class)
    @Column(length = 1000)
    private String description;

    @Column(nullable = false, length = 50)
    private String method;

    @Column(nullable = false, length = 500)
    private String path;

    /**
     * Public-facing masked path. Clients call this path,
     * gateway internally routes to the real targetUrl.
     * If null, uses 'path' as-is without masking.
     */
    @Column(length = 500)
    private String maskedPath;

    @Convert(converter = FieldEncryptor.class)
    @Column(nullable = false, length = 1000)
    private String targetUrl;

    @Column(nullable = false)
    private boolean authRequired;

    @Column(nullable = false)
    private boolean rateLimitEnabled;

    @Column(nullable = false)
    private int rateLimitPerMinute;

    @Column(nullable = false)
    private boolean active;

    /**
     * Enable end-to-end payload encryption for this route.
     * When enabled, request/response bodies are encrypted with client's key.
     */
    @Column(nullable = false)
    private boolean payloadEncryption;

    /**
     * Health check URL for live monitoring.
     * Gateway pings this endpoint to determine service status.
     */
    @Column(length = 500)
    private String healthCheckUrl;

    /**
     * Current health status: UP, DOWN, DEGRADED, UNKNOWN
     */
    @Column(length = 20)
    @Builder.Default
    private String healthStatus = "UNKNOWN";

    /**
     * Last time health was checked
     */
    private LocalDateTime lastHealthCheck;

    /**
     * Response time in ms from last health check
     */
    private Integer lastResponseTimeMs;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        active = true;
        if (rateLimitPerMinute == 0) rateLimitPerMinute = 60;
        if (healthStatus == null) healthStatus = "UNKNOWN";
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

