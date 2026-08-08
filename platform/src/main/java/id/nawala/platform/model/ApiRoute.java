package id.nawala.platform.model;

import id.nawala.platform.util.FieldEncryptor;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    @Column(nullable = false)
    private boolean payloadEncryption;

    @Column(length = 500)
    private String healthCheckUrl;

    @Column(length = 20)
    @Builder.Default
    private String healthStatus = "UNKNOWN";

    private LocalDateTime lastHealthCheck;

    private Integer lastResponseTimeMs;

    // === LOAD BALANCER FIELDS ===
    @Column(nullable = false)
    @Builder.Default
    private boolean loadBalanced = false;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private LoadBalancerStrategy loadBalancerStrategy = LoadBalancerStrategy.ROUND_ROBIN;

    @OneToMany(mappedBy = "route", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<RouteTarget> additionalTargets = new ArrayList<>();

    // Current index for round-robin (transient, not persisted)
    @Transient
    private int currentTargetIndex = 0;

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
        if (loadBalancerStrategy == null) loadBalancerStrategy = LoadBalancerStrategy.ROUND_ROBIN;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Get next target URL based on load balancer strategy
     */
    public String getNextTargetUrl() {
        if (!loadBalanced || additionalTargets == null || additionalTargets.isEmpty()) {
            return targetUrl;
        }

        List<String> allTargets = new ArrayList<>();
        allTargets.add(targetUrl);
        additionalTargets.stream()
            .filter(RouteTarget::isHealthy)
            .forEach(t -> allTargets.add(t.getUrl()));

        if (allTargets.size() == 1) {
            return targetUrl;
        }

        return switch (loadBalancerStrategy) {
            case ROUND_ROBIN -> {
                currentTargetIndex = (currentTargetIndex + 1) % allTargets.size();
                yield allTargets.get(currentTargetIndex);
            }
            case RANDOM -> allTargets.get((int) (Math.random() * allTargets.size()));
            case WEIGHTED -> selectWeightedTarget(allTargets);
            default -> targetUrl;
        };
    }

    private String selectWeightedTarget(List<String> targets) {
        // Simple weighted selection based on configured weights
        int totalWeight = 100; // Primary target weight
        for (RouteTarget t : additionalTargets) {
            if (t.isHealthy()) totalWeight += t.getWeight();
        }
        
        int random = (int) (Math.random() * totalWeight);
        int cumulative = 100;
        
        if (random < cumulative) return targetUrl;
        
        for (RouteTarget t : additionalTargets) {
            if (t.isHealthy()) {
                cumulative += t.getWeight();
                if (random < cumulative) return t.getUrl();
            }
        }
        return targetUrl;
    }
}
