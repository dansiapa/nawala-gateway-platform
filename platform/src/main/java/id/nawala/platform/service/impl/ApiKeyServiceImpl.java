package id.nawala.platform.service.impl;

import id.nawala.platform.logging.SecurityLogger;
import id.nawala.platform.model.ApiKey;
import id.nawala.platform.model.User;
import id.nawala.platform.repository.ApiKeyRepository;
import id.nawala.platform.service.ApiKeyService;
import id.nawala.platform.service.WebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ApiKeyServiceImpl implements ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;
    private final PasswordEncoder passwordEncoder;
    private final WebhookService webhookService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public ApiKey generate(String name, User owner, Integer expirationDays) {
        return generateWithScope(name, owner, expirationDays, 0, 0, null, null, null);
    }

    @Override
    public ApiKey generateWithScope(String name, User owner, Integer expirationDays,
                                    long dailyQuota, long monthlyQuota,
                                    String allowedIps, String allowedRoutes, String allowedMethods) {
        String rawKey = generateRawKey();
        String prefix = rawKey.substring(0, 8);
        String keyHash = passwordEncoder.encode(rawKey);

        ApiKey apiKey = ApiKey.builder()
                .name(name).keyHash(keyHash).prefix(prefix).owner(owner).active(true)
                .expiresAt(expirationDays != null ? LocalDateTime.now().plusDays(expirationDays) : null)
                .dailyQuota(dailyQuota).monthlyQuota(monthlyQuota)
                .allowedIps(allowedIps).allowedRoutes(allowedRoutes).allowedMethods(allowedMethods)
                .build();

        ApiKey saved = apiKeyRepository.save(apiKey);
        saved.setKeyHash(rawKey); // Return raw key only during creation
        SecurityLogger.log().info("API key created name={} owner={} prefix={}", name, owner.getUsername(), prefix);
        return saved;
    }

    @Override
    public ApiKey rotate(Long keyId) {
        ApiKey existing = apiKeyRepository.findById(keyId)
                .orElseThrow(() -> new IllegalArgumentException("API Key not found"));

        // Store old key hash for grace period
        existing.setPreviousKeyHash(existing.getKeyHash());
        existing.setRotationGraceUntil(LocalDateTime.now().plusHours(24));

        // Generate new key
        String rawKey = generateRawKey();
        existing.setKeyHash(passwordEncoder.encode(rawKey));
        existing.setPrefix(rawKey.substring(0, 8));

        ApiKey saved = apiKeyRepository.save(existing);
        saved.setKeyHash(rawKey);
        SecurityLogger.log().info("API key rotated id={} newPrefix={}", keyId, rawKey.substring(0, 8));
        return saved;
    }

    @Override
    public void revoke(Long id) {
        apiKeyRepository.findById(id).ifPresent(key -> {
            key.setActive(false);
            apiKeyRepository.save(key);
            SecurityLogger.log().info("API key revoked id={} prefix={}", id, key.getPrefix());
        });
    }

    @Override
    @Transactional(readOnly = true)
    public boolean validate(String rawKey) {
        return validateInternal(rawKey) != null;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean validateWithScope(String rawKey, String ip, String method, Long routeId) {
        ApiKey key = validateInternal(rawKey);
        if (key == null) return false;
        if (!key.isIpAllowed(ip)) {
            SecurityLogger.log().warn("API key IP denied prefix={} ip={}", key.getPrefix(), ip);
            return false;
        }
        if (!key.isMethodAllowed(method)) {
            SecurityLogger.log().warn("API key method denied prefix={} method={}", key.getPrefix(), method);
            return false;
        }
        if (routeId != null && !key.isRouteAllowed(routeId)) {
            SecurityLogger.log().warn("API key route denied prefix={} routeId={}", key.getPrefix(), routeId);
            return false;
        }
        if (key.isQuotaExceeded()) {
            SecurityLogger.log().warn("API key quota exceeded prefix={}", key.getPrefix());
            webhookService.fireEvent("QUOTA_REACHED",
                    "{\"keyPrefix\":\"" + key.getPrefix() + "\",\"name\":\"" + key.getName() + "\"}");
            return false;
        }
        return true;
    }

    private ApiKey validateInternal(String rawKey) {
        if (rawKey == null || rawKey.length() < 8) return null;
        String prefix = rawKey.substring(0, 8);
        return apiKeyRepository.findByPrefix(prefix)
                .filter(key -> key.isActive() && !key.isExpired())
                .filter(key -> passwordEncoder.matches(rawKey, key.getKeyHash())
                        || isGracePeriodValid(key, rawKey))
                .orElse(null);
    }

    private boolean isGracePeriodValid(ApiKey key, String rawKey) {
        if (key.getPreviousKeyHash() == null || key.getRotationGraceUntil() == null) return false;
        if (LocalDateTime.now().isAfter(key.getRotationGraceUntil())) return false;
        return passwordEncoder.matches(rawKey, key.getPreviousKeyHash());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApiKey> findByOwner(User owner) {
        return apiKeyRepository.findByOwner(owner);
    }

    @Override
    @Transactional(readOnly = true)
    public long getActiveKeyCount() {
        return apiKeyRepository.countByActiveTrue();
    }

    @Override
    public void incrementUsage(String keyPrefix) {
        apiKeyRepository.findByPrefix(keyPrefix).ifPresent(key -> {
            key.setRequestCount(key.getRequestCount() + 1);
            key.setDailyUsage(key.getDailyUsage() + 1);
            key.setMonthlyUsage(key.getMonthlyUsage() + 1);
            key.setLastUsedAt(LocalDateTime.now());
            apiKeyRepository.save(key);
        });
    }

    @Override
    @Scheduled(cron = "0 0 0 * * *") // midnight daily
    public void resetDailyQuotas() {
        apiKeyRepository.findAll().forEach(key -> {
            key.setDailyUsage(0);
            key.setLastQuotaReset(LocalDateTime.now());
            apiKeyRepository.save(key);
        });
    }

    @Override
    @Scheduled(cron = "0 0 0 1 * *") // first of month
    public void resetMonthlyQuotas() {
        apiKeyRepository.findAll().forEach(key -> {
            key.setMonthlyUsage(0);
            apiKeyRepository.save(key);
        });
    }

    private String generateRawKey() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return "nwl_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
