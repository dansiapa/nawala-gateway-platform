package id.nawala.platform.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Webhook delivery log for tracking delivery attempts.
 */
@Entity
@Table(name = "webhook_deliveries")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "webhook_id", nullable = false)
    private Webhook webhook;

    @Column(nullable = false)
    private String eventType;

    @Column(columnDefinition = "TEXT")
    private String payload;

    private int httpStatus;
    private String responseBody;
    private long durationMs;

    private int attemptNumber;

    @Builder.Default
    private String status = "PENDING"; // PENDING, SUCCESS, FAILED, RETRYING

    private LocalDateTime deliveredAt;
    private LocalDateTime nextRetryAt;

    @PrePersist
    protected void onCreate() {
        if (deliveredAt == null) deliveredAt = LocalDateTime.now();
    }
}
