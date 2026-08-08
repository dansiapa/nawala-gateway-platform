package id.nawala.platform.service;

import id.nawala.platform.model.Role;
import id.nawala.platform.model.SystemSettings;
import id.nawala.platform.model.User;
import id.nawala.platform.repository.SystemSettingsRepository;
import id.nawala.platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SetupService {

    private final SystemSettingsRepository settingsRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Check if initial setup has been completed
     */
    public boolean isSetupCompleted() {
        return settingsRepository.findBySettingKey(SystemSettings.SETUP_COMPLETED)
                .map(s -> "true".equals(s.getSettingValue()))
                .orElse(false);
    }

    /**
     * Check if any admin user exists (alternative check)
     */
    public boolean hasAdminUser() {
        return userRepository.existsByRole(Role.ADMIN);
    }

    /**
     * Get a setting value
     */
    public Optional<String> getSetting(String key) {
        return settingsRepository.findBySettingKey(key)
                .map(SystemSettings::getSettingValue);
    }

    /**
     * Set a setting value
     */
    @Transactional
    public void setSetting(String key, String value, String description, boolean encrypted) {
        SystemSettings settings = settingsRepository.findBySettingKey(key)
                .orElse(SystemSettings.builder()
                        .settingKey(key)
                        .build());
        settings.setSettingValue(value);
        if (description != null) settings.setDescription(description);
        settings.setEncrypted(encrypted);
        settingsRepository.save(settings);
    }

    /**
     * Complete the setup wizard
     */
    @Transactional
    public User completeSetup(SetupRequest request) {
        log.info("Starting initial setup...");

        // 1. Create admin user
        User admin = User.builder()
                .username(request.adminUsername())
                .email(request.adminEmail())
                .password(passwordEncoder.encode(request.adminPassword()))
                .fullName(request.adminFullName())
                .role(Role.ADMIN)
                .enabled(true)
                .canManageRoutes(true)
                .canManageKeys(true)
                .canViewAnalytics(true)
                .canManageUsers(true)
                .canManageWaf(true)
                .build();
        admin = userRepository.save(admin);
        log.info("Admin user created: {}", admin.getUsername());

        // 2. Save platform settings
        setSetting(SystemSettings.PLATFORM_NAME, request.platformName(), "Platform display name", false);
        setSetting(SystemSettings.PLATFORM_URL, request.platformUrl(), "Platform base URL", false);
        setSetting(SystemSettings.DB_TYPE, request.dbType(), "Database type", false);

        // 3. Save SMTP settings if provided
        if (request.smtpEnabled() && request.smtpHost() != null) {
            setSetting(SystemSettings.SMTP_ENABLED, "true", "SMTP enabled", false);
            setSetting(SystemSettings.SMTP_HOST, request.smtpHost(), "SMTP host", false);
            setSetting(SystemSettings.SMTP_PORT, String.valueOf(request.smtpPort()), "SMTP port", false);
            if (request.smtpUser() != null) {
                setSetting(SystemSettings.SMTP_USER, request.smtpUser(), "SMTP username", false);
            }
            if (request.smtpPass() != null) {
                setSetting(SystemSettings.SMTP_PASS, request.smtpPass(), "SMTP password", true);
            }
        } else {
            setSetting(SystemSettings.SMTP_ENABLED, "false", "SMTP enabled", false);
        }

        // 4. Mark setup as completed
        setSetting(SystemSettings.SETUP_COMPLETED, "true", "Initial setup completed", false);
        setSetting(SystemSettings.SETUP_COMPLETED_AT, LocalDateTime.now().toString(), "Setup completion time", false);

        log.info("Initial setup completed successfully!");
        return admin;
    }

    /**
     * Setup request record
     */
    public record SetupRequest(
            // Step 1: Platform info
            String platformName,
            String platformUrl,
            // Step 2: Database (already configured via properties, just store type)
            String dbType,
            // Step 3: Admin account
            String adminUsername,
            String adminEmail,
            String adminPassword,
            String adminFullName,
            // Step 4: SMTP (optional)
            boolean smtpEnabled,
            String smtpHost,
            int smtpPort,
            String smtpUser,
            String smtpPass
    ) {}
}
