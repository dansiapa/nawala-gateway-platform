package id.nawala.platform.datacenter;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * Multi-datacenter configuration entity
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "datacenters")
public class DatacenterConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    @Column(nullable = false)
    private String region;

    @Column(nullable = false)
    private String endpoint;

    private int port = 8080;

    @Column(nullable = false)
    private boolean primary = false;

    private boolean enabled = true;

    private int weight = 100; // For load balancing

    @Enumerated(EnumType.STRING)
    private DatacenterStatus status = DatacenterStatus.HEALTHY;

    private LocalDateTime lastHealthCheck;

    private long latencyMs;

    @Column(length = 500)
    private String healthEndpoint = "/health";

    private String apiKey;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum DatacenterStatus {
        HEALTHY, DEGRADED, UNHEALTHY, MAINTENANCE
    }
}
