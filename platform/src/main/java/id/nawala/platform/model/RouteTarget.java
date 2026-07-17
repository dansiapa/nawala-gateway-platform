package id.nawala.platform.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Load balancer target for a route.
 * Multiple targets per route enable round-robin/weighted routing.
 */
@Entity
@Table(name = "route_targets")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteTarget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    private ApiRoute route;

    @Column(nullable = false)
    private String targetUrl;

    @Builder.Default
    private int weight = 100; // percentage weight for weighted routing

    @Builder.Default
    private String strategy = "ROUND_ROBIN"; // ROUND_ROBIN, WEIGHTED, LEAST_CONN

    @Builder.Default
    private boolean healthy = true;

    @Builder.Default
    private boolean active = true;

    private int consecutiveFailures;
    private LocalDateTime lastHealthCheck;
    private Long lastResponseTimeMs;

    // Canary deployment support
    @Builder.Default
    private boolean canary = false;

    private int canaryPercentage; // 0-100

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
