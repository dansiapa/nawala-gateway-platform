package id.nawala.platform.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "system_settings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 100)
    private String settingKey;

    @Column(length = 2000)
    private String settingValue;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    @Builder.Default
    private boolean encrypted = false;

    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Common setting keys
    public static final String SETUP_COMPLETED = "setup.completed";
    public static final String SETUP_COMPLETED_AT = "setup.completed_at";
    public static final String PLATFORM_NAME = "platform.name";
    public static final String PLATFORM_URL = "platform.url";
    public static final String DB_TYPE = "database.type";
    public static final String SMTP_ENABLED = "smtp.enabled";
    public static final String SMTP_HOST = "smtp.host";
    public static final String SMTP_PORT = "smtp.port";
    public static final String SMTP_USER = "smtp.user";
    public static final String SMTP_PASS = "smtp.password";
}
