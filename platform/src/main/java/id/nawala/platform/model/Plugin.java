package id.nawala.platform.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Plugin / Extension registration.
 * Supports custom request/response processing hooks.
 */
@Entity
@Table(name = "plugins")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Plugin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** Plugin type: PRE_REQUEST, POST_RESPONSE, ERROR_HANDLER, SCHEDULER */
    @Column(nullable = false, length = 30)
    private String hookType;

    /** Script/logic in JavaScript or Lua */
    @Column(columnDefinition = "LONGTEXT")
    private String script;

    /** Target route ID (null = global) */
    private Long routeId;

    @Builder.Default
    private int priority = 100;

    @Builder.Default
    private boolean active = true;

    private long executionCount;
    private long errorCount;
    private Long avgExecutionTimeMs;

    private LocalDateTime createdAt;
    private LocalDateTime lastExecutedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
