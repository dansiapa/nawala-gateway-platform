package id.nawala.platform.config;

import id.nawala.platform.util.EncryptionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EncryptionConfig {

    @Value("${nawala.encryption.key}")
    private String encryptionKey;

    @Bean
    public EncryptionUtils encryptionUtils() {
        return new EncryptionUtils(encryptionKey);
    }
}
