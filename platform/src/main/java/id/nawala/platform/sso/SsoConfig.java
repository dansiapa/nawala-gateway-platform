package id.nawala.platform.sso;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "sso_configs")
public class SsoConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SsoType type; // SAML, LDAP, OIDC

    private boolean enabled = true;
    private boolean isDefault = false;

    // SAML Settings
    private String samlEntityId;
    private String samlMetadataUrl;
    @Column(length = 4000)
    private String samlCertificate;
    private String samlAcsUrl;
    private String samlSloUrl;

    // LDAP Settings
    private String ldapUrl;
    private String ldapBaseDn;
    private String ldapUserDn;
    private String ldapPassword;
    private String ldapUserFilter;
    private String ldapGroupFilter;

    // OIDC Settings
    private String oidcIssuer;
    private String oidcClientId;
    private String oidcClientSecret;
    private String oidcScopes;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum SsoType {
        SAML, LDAP, OIDC
    }
}
