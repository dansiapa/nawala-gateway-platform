package id.nawala.platform.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Mock endpoint definition for API sandbox/testing.
 */
@Entity
@Table(name = "api_mocks")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiMock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User owner;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String path; // e.g. /mock/users

    @Column(nullable = false)
    private String method; // GET, POST, PUT, DELETE

    private int statusCode;

    @Column(columnDefinition = "TEXT")
    private String responseBody;

    private String contentType; // application/json

    private int delayMs; // simulated latency

    @Builder.Default
    private boolean active = true;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
