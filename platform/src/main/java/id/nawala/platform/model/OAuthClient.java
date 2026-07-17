package id.nawala.platform.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * OAuth2 Client registration for client_credentials flow.
 */
@Entity
@Table(name = "oauth2_clients")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OAuthClient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 64)
    private String clientId;

    @Column(nullable = false, length = 255)
    private String clientSecretHash;

    @Column(nullable = false, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    /** Comma-separated grant types: client_credentials, authorization_code, refresh_token */
    @Column(nullable = false)
    @Builder.Default
    private String grantTypes = "client_credentials";

    /** Comma-separated scopes: read, write, admin */
    @Column(nullable = false)
    @Builder.Default
    private String scopes = "read";

    /** Redirect URIs for authorization_code (comma-separated) */
    @Column(length = 2000)
    private String redirectUris;

    /** Access token TTL in seconds */
    @Builder.Default
    private int accessTokenTtl = 3600;

    /** Refresh token TTL in seconds */
    @Builder.Default
    private int refreshTokenTtl = 86400;

    @Builder.Default
    private boolean active = true;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
