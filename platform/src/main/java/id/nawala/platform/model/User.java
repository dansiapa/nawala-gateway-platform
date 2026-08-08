package id.nawala.platform.model;

import id.nawala.platform.util.FieldEncryptor;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(nullable = false)
    private String password;

    @Convert(converter = FieldEncryptor.class)
    @Column(unique = true, nullable = false, length = 500)
    private String email;

    @Convert(converter = FieldEncryptor.class)
    @Column(length = 500)
    private String fullName;

    @Convert(converter = FieldEncryptor.class)
    @Column(length = 500)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(nullable = false)
    private boolean enabled;

    // === PRIVILEGES ===
    @Column(nullable = false)
    @Builder.Default
    private boolean canManageRoutes = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean canManageKeys = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean canViewAnalytics = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean canManageUsers = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean canManageWaf = false;

    // === PREFERENCES ===
    @Column(length = 20)
    @Builder.Default
    private String themePreference = "system";

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime lastLoginAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (role == null) role = Role.USER;
        enabled = true;
        if (themePreference == null) themePreference = "system";
        
        // Admin gets all privileges by default
        if (role == Role.ADMIN) {
            canManageRoutes = true;
            canManageKeys = true;
            canViewAnalytics = true;
            canManageUsers = true;
            canManageWaf = true;
        }
    }
    
    /**
     * Check if user has specific privilege
     */
    public boolean hasPrivilege(String privilege) {
        if (role == Role.ADMIN) return true;
        return switch (privilege) {
            case "routes" -> canManageRoutes;
            case "keys" -> canManageKeys;
            case "analytics" -> canViewAnalytics;
            case "users" -> canManageUsers;
            case "waf" -> canManageWaf;
            default -> false;
        };
    }
}
