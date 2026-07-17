package id.nawala.platform.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "api_keys")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(unique = true, nullable = false, length = 255)
    private String keyHash;

    @Column(length = 20)
    private String prefix;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User owner;

    @Column(nullable = false)
    private boolean active;

    private LocalDateTime expiresAt;

    private long requestCount;

    // === Lifecycle Management ===

    /** Daily request quota. 0 = unlimited */
    @Builder.Default
    private long dailyQuota = 0;

    /** Requests made today */
    @Builder.Default
    private long dailyUsage = 0;

    /** Monthly request quota. 0 = unlimited */
    @Builder.Default
    private long monthlyQuota = 0;

    /** Requests made this month */
    @Builder.Default
    private long monthlyUsage = 0;

    /** Comma-separated allowed IP addresses. Empty = all allowed */
    @Column(length = 2000)
    private String allowedIps;

    /** Comma-separated allowed route IDs. Empty = all routes */
    @Column(length = 2000)
    private String allowedRoutes;

    /** Comma-separated allowed methods (GET,POST,PUT,DELETE). Empty = all */
    @Column(length = 200)
    private String allowedMethods;

    /** Previous key hash for rotation grace period */
    @Column(length = 255)
    private String previousKeyHash;

    /** Grace period expiry for old key during rotation */
    private LocalDateTime rotationGraceUntil;

    /** Last quota reset date */
    private LocalDateTime lastQuotaReset;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime lastUsedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        active = true;
        requestCount = 0;
    }

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isQuotaExceeded() {
        if (dailyQuota > 0 && dailyUsage >= dailyQuota) return true;
        if (monthlyQuota > 0 && monthlyUsage >= monthlyQuota) return true;
        return false;
    }

    public boolean isIpAllowed(String ip) {
        if (allowedIps == null || allowedIps.isBlank()) return true;
        String[] ips = allowedIps.split(",");
        for (String allowed : ips) {
            if (allowed.trim().equals(ip)) return true;
        }
        return false;
    }

    public boolean isRouteAllowed(Long routeId) {
        if (allowedRoutes == null || allowedRoutes.isBlank()) return true;
        String[] routes = allowedRoutes.split(",");
        for (String r : routes) {
            if (r.trim().equals(String.valueOf(routeId))) return true;
        }
        return false;
    }

    public boolean isMethodAllowed(String method) {
        if (allowedMethods == null || allowedMethods.isBlank()) return true;
        String[] methods = allowedMethods.split(",");
        for (String m : methods) {
            if (m.trim().equalsIgnoreCase(method)) return true;
        }
        return false;
    }
}

