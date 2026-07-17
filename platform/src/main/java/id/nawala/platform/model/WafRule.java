package id.nawala.platform.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * WAF rule definition for request filtering.
 */
@Entity
@Table(name = "waf_rules")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WafRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String ruleType; // SQL_INJECTION, XSS, PATH_TRAVERSAL, CUSTOM

    @Column(columnDefinition = "TEXT")
    private String pattern; // regex pattern

    @Column(nullable = false)
    private String action; // BLOCK, LOG, CHALLENGE

    private String targetField; // HEADER, BODY, QUERY_PARAM, PATH, ALL

    private Long routeId; // null = global rule

    @Builder.Default
    private int priority = 100; // lower = higher priority

    @Builder.Default
    private boolean active = true;

    private String description;

    private long matchCount;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
