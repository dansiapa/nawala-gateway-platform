package id.nawala.platform.service;

import id.nawala.platform.model.ApiKey;
import id.nawala.platform.model.User;

import java.util.List;

public interface ApiKeyService {

    ApiKey generate(String name, User owner, Integer expirationDays);

    ApiKey generateWithScope(String name, User owner, Integer expirationDays,
                             long dailyQuota, long monthlyQuota,
                             String allowedIps, String allowedRoutes, String allowedMethods);

    ApiKey rotate(Long keyId);

    void revoke(Long id);

    boolean validate(String rawKey);

    boolean validateWithScope(String rawKey, String ip, String method, Long routeId);

    List<ApiKey> findByOwner(User owner);

    long getActiveKeyCount();

    void incrementUsage(String keyHash);

    void resetDailyQuotas();

    void resetMonthlyQuotas();
}

