package id.nawala.gateway.filter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthFilter {
    
    private final JdbcTemplate jdbcTemplate;
    private final Map<String, ApiKeyInfo> keyCache = new ConcurrentHashMap<>();
    
    public Optional<ApiKeyInfo> validateApiKey(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return Optional.empty();
        }
        
        // Check cache first
        ApiKeyInfo cached = keyCache.get(apiKey);
        if (cached != null && !cached.isExpired()) {
            return Optional.of(cached);
        }
        
        try {
            String hashedKey = hashKey(apiKey);
            
            return jdbcTemplate.query(
                "SELECT ak.id, ak.name, ak.owner_id, ak.rate_limit_per_minute, ak.expires_at, u.username " +
                "FROM api_keys ak JOIN users u ON ak.owner_id = u.id " +
                "WHERE ak.key_hash = ? AND ak.active = true AND ak.revoked = false " +
                "AND (ak.expires_at IS NULL OR ak.expires_at > NOW())",
                rs -> {
                    if (rs.next()) {
                        ApiKeyInfo info = new ApiKeyInfo(
                            rs.getLong("id"),
                            rs.getString("name"),
                            rs.getLong("owner_id"),
                            rs.getString("username"),
                            rs.getInt("rate_limit_per_minute"),
                            rs.getTimestamp("expires_at") != null ? rs.getTimestamp("expires_at").getTime() : Long.MAX_VALUE,
                            System.currentTimeMillis() + 60_000 // Cache for 1 minute
                        );
                        keyCache.put(apiKey, info);
                        
                        // Update last used
                        jdbcTemplate.update("UPDATE api_keys SET last_used_at = NOW() WHERE id = ?", info.keyId());
                        
                        return Optional.of(info);
                    }
                    return Optional.empty();
                },
                hashedKey
            );
        } catch (Exception e) {
            log.error("Error validating API key: {}", e.getMessage());
            return Optional.empty();
        }
    }
    
    public Optional<OAuthTokenInfo> validateOAuthToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        
        try {
            return jdbcTemplate.query(
                "SELECT ot.id, ot.client_id, ot.scopes, ot.expires_at, oc.name as client_name, oc.owner_id " +
                "FROM oauth_tokens ot JOIN oauth_clients oc ON ot.client_id = oc.id " +
                "WHERE ot.access_token = ? AND ot.expires_at > NOW() AND ot.revoked = false",
                rs -> {
                    if (rs.next()) {
                        return Optional.of(new OAuthTokenInfo(
                            rs.getLong("id"),
                            rs.getLong("client_id"),
                            rs.getString("client_name"),
                            rs.getLong("owner_id"),
                            rs.getString("scopes")
                        ));
                    }
                    return Optional.empty();
                },
                token
            );
        } catch (Exception e) {
            log.error("Error validating OAuth token: {}", e.getMessage());
            return Optional.empty();
        }
    }
    
    private String hashKey(String key) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(key.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return key;
        }
    }
    
    public void invalidateCache(String apiKey) {
        keyCache.remove(apiKey);
    }
    
    public record ApiKeyInfo(Long keyId, String name, Long ownerId, String ownerUsername, 
                             int rateLimit, long expiresAt, long cacheExpiry) {
        public boolean isExpired() {
            return System.currentTimeMillis() > cacheExpiry;
        }
    }
    
    public record OAuthTokenInfo(Long tokenId, Long clientId, String clientName, Long ownerId, String scopes) {}
}
