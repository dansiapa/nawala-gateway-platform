package id.nawala.platform.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * API Documentation spec storage per route.
 */
@Entity
@Table(name = "api_docs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiDoc {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id")
    private ApiRoute route;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 20)
    @Builder.Default
    private String version = "1.0.0";

    @Column(columnDefinition = "LONGTEXT")
    private String openApiSpec; // OpenAPI/Swagger JSON or YAML

    @Column(columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    private boolean published = false;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
