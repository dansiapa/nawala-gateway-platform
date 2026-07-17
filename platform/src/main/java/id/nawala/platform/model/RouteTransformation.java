package id.nawala.platform.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Request/Response transformation rule per route.
 */
@Entity
@Table(name = "route_transformations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteTransformation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    private ApiRoute route;

    @Column(nullable = false)
    private String phase; // REQUEST, RESPONSE

    @Column(nullable = false)
    private String type; // ADD_HEADER, REMOVE_HEADER, RENAME_HEADER, BODY_TRANSFORM, FIELD_REDACT, JSON_SCHEMA_VALIDATE

    private String key; // header name or field path
    private String value; // header value, replacement, or schema

    @Column(columnDefinition = "TEXT")
    private String config; // JSON config for complex transforms

    @Builder.Default
    private int priority = 100;

    @Builder.Default
    private boolean active = true;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
