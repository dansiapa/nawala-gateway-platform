package id.nawala.platform.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

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
    @ToString.Exclude
    private ApiRoute route;

    @Column(nullable = false, length = 500)
    private String url;

    @Builder.Default
    private int weight = 50;

    @Builder.Default
    private boolean healthy = true;

    @Builder.Default
    private boolean active = true;

    private int consecutiveFailures;
    private LocalDateTime lastHealthCheck;
    private Long lastResponseTimeMs;

    @Builder.Default
    private boolean canary = false;

    private int canaryPercentage;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
