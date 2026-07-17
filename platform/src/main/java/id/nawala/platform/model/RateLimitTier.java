package id.nawala.platform.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Rate limit tier configuration.
 */
@Entity
@Table(name = "rate_limit_tiers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateLimitTier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String name;

    @Column(nullable = false)
    private int requestsPerMinute;

    @Column(nullable = false)
    private int requestsPerHour;

    @Column(nullable = false)
    private int requestsPerDay;

    @Builder.Default
    private int burstSize = 10;

    @Column(length = 200)
    private String description;

    @Builder.Default
    private boolean active = true;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
