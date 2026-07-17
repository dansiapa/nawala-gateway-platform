package id.nawala.platform.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Webhook configuration for event notifications.
 */
@Entity
@Table(name = "webhooks")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Webhook {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User owner;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String targetUrl;

    @Column(nullable = false)
    private String eventType; // ROUTE_DOWN, ANOMALY_DETECTED, KEY_EXPIRED, QUOTA_REACHED

    private String secret; // HMAC signing secret

    @Builder.Default
    private boolean active = true;

    @Builder.Default
    private int maxRetries = 3;

    private LocalDateTime lastTriggeredAt;
    private String lastStatus; // SUCCESS, FAILED

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
