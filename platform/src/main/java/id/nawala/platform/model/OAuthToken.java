package id.nawala.platform.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * OAuth2 Access/Refresh token storage.
 */
@Entity
@Table(name = "oauth2_tokens", indexes = {
    @Index(name = "idx_token_access", columnList = "accessToken"),
    @Index(name = "idx_token_refresh", columnList = "refreshToken")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OAuthToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private OAuthClient client;

    @Column(unique = true, nullable = false, length = 512)
    private String accessToken;

    @Column(unique = true, length = 512)
    private String refreshToken;

    private String scopes;

    private LocalDateTime accessTokenExpiresAt;
    private LocalDateTime refreshTokenExpiresAt;

    @Builder.Default
    private boolean revoked = false;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public boolean isAccessTokenExpired() {
        return LocalDateTime.now().isAfter(accessTokenExpiresAt);
    }

    public boolean isRefreshTokenExpired() {
        return refreshTokenExpiresAt != null && LocalDateTime.now().isAfter(refreshTokenExpiresAt);
    }
}
