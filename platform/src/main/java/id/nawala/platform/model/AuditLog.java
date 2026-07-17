package id.nawala.platform.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Audit trail for all significant operations.
 */
@Entity
@Table(name = "audit_log", indexes = {
    @Index(name = "idx_audit_user", columnList = "userId,createdAt"),
    @Index(name = "idx_audit_action", columnList = "action,createdAt")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private String username;

    @Column(nullable = false, length = 50)
    private String action; // CREATE, UPDATE, DELETE, LOGIN, REVOKE, ROTATE, etc.

    @Column(length = 50)
    private String resourceType; // ROUTE, API_KEY, WEBHOOK, WAF_RULE, etc.

    private Long resourceId;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(length = 45)
    private String ipAddress;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
