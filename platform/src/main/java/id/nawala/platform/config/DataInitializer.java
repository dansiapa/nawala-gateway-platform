package id.nawala.platform.config;

import id.nawala.platform.model.Role;
import id.nawala.platform.model.SystemSettings;
import id.nawala.platform.repository.SystemSettingsRepository;
import id.nawala.platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final SystemSettingsRepository settingsRepository;

    @Override
    public void run(String... args) {
        // Check if setup is completed
        boolean setupCompleted = settingsRepository.findBySettingKey(SystemSettings.SETUP_COMPLETED)
                .map(s -> "true".equals(s.getSettingValue()))
                .orElse(false);
        
        boolean hasAdmin = userRepository.existsByRole(Role.ADMIN);
        
        if (!setupCompleted && !hasAdmin) {
            log.info("============================================");
            log.info("  NAWALA GATEWAY - FIRST RUN DETECTED");
            log.info("  Please visit /setup to complete setup");
            log.info("============================================");
        } else if (setupCompleted) {
            String platformName = settingsRepository.findBySettingKey(SystemSettings.PLATFORM_NAME)
                    .map(SystemSettings::getSettingValue)
                    .orElse("Nawala Gateway");
            log.info("============================================");
            log.info("  {} - Ready", platformName);
            log.info("============================================");
        }
    }
}
