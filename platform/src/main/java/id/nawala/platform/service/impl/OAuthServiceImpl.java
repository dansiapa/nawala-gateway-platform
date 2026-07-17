package id.nawala.platform.service.impl;

import id.nawala.platform.logging.SecurityLogger;
import id.nawala.platform.model.OAuthClient;
import id.nawala.platform.model.OAuthToken;
import id.nawala.platform.model.User;
import id.nawala.platform.repository.OAuthClientRepository;
import id.nawala.platform.repository.OAuthTokenRepository;
import id.nawala.platform.repository.UserRepository;
import id.nawala.platform.service.OAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class OAuthServiceImpl implements OAuthService {

    private final OAuthClientRepository clientRepository;
    private final OAuthTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional
    public OAuthClient registerClient(Long userId, String name, String grantTypes, String scopes, String redirectUris) {
        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        String clientId = generateClientId();
        String clientSecret = generateSecret();
        OAuthClient client = OAuthClient.builder()
                .clientId(clientId)
                .clientSecretHash(passwordEncoder.encode(clientSecret))
                .name(name).owner(owner)
                .grantTypes(grantTypes != null ? grantTypes : "client_credentials")
                .scopes(scopes != null ? scopes : "read")
                .redirectUris(redirectUris)
                .active(true)
                .build();
        OAuthClient saved = clientRepository.save(client);
        // Temporarily set plaintext secret for display (not persisted)
        saved.setClientSecretHash(clientSecret);
        SecurityLogger.log().info("OAuth client registered clientId={} owner={}", clientId, owner.getUsername());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<OAuthClient> getClientsByUser(Long userId) {
        return clientRepository.findByOwnerId(userId);
    }

    @Override
    @Transactional
    public void deleteClient(Long clientId) {
        clientRepository.deleteById(clientId);
    }

    @Override
    @Transactional
    public Map<String, Object> issueToken(String clientId, String clientSecret, String grantType, String scope) {
        OAuthClient client = clientRepository.findByClientIdAndActiveTrue(clientId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid client"));
        if (!passwordEncoder.matches(clientSecret, client.getClientSecretHash())) {
            SecurityLogger.log().warn("OAuth invalid secret clientId={}", clientId);
            throw new IllegalArgumentException("Invalid client credentials");
        }
        if (!client.getGrantTypes().contains(grantType)) {
            throw new IllegalArgumentException("Unsupported grant type: " + grantType);
        }
        String resolvedScope = resolveScope(scope, client.getScopes());
        String accessToken = generateToken();
        String refreshToken = client.getGrantTypes().contains("refresh_token") ? generateToken() : null;
        OAuthToken token = OAuthToken.builder()
                .client(client)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .scopes(resolvedScope)
                .accessTokenExpiresAt(LocalDateTime.now().plusSeconds(client.getAccessTokenTtl()))
                .refreshTokenExpiresAt(refreshToken != null ? LocalDateTime.now().plusSeconds(client.getRefreshTokenTtl()) : null)
                .build();
        tokenRepository.save(token);
        SecurityLogger.log().info("OAuth token issued clientId={} scope={}", clientId, resolvedScope);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("access_token", accessToken);
        result.put("token_type", "Bearer");
        result.put("expires_in", client.getAccessTokenTtl());
        result.put("scope", resolvedScope);
        if (refreshToken != null) result.put("refresh_token", refreshToken);
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> refreshToken(String refreshToken) {
        OAuthToken existing = tokenRepository.findByRefreshTokenAndRevokedFalse(refreshToken)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));
        if (existing.isRefreshTokenExpired()) {
            existing.setRevoked(true);
            tokenRepository.save(existing);
            throw new IllegalArgumentException("Refresh token expired");
        }
        existing.setRevoked(true);
        tokenRepository.save(existing);
        OAuthClient client = existing.getClient();
        String newAccess = generateToken();
        String newRefresh = generateToken();
        OAuthToken newToken = OAuthToken.builder()
                .client(client)
                .accessToken(newAccess).refreshToken(newRefresh)
                .scopes(existing.getScopes())
                .accessTokenExpiresAt(LocalDateTime.now().plusSeconds(client.getAccessTokenTtl()))
                .refreshTokenExpiresAt(LocalDateTime.now().plusSeconds(client.getRefreshTokenTtl()))
                .build();
        tokenRepository.save(newToken);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("access_token", newAccess);
        result.put("token_type", "Bearer");
        result.put("expires_in", client.getAccessTokenTtl());
        result.put("scope", existing.getScopes());
        result.put("refresh_token", newRefresh);
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public TokenInfo validateToken(String accessToken) {
        return tokenRepository.findByAccessTokenAndRevokedFalse(accessToken)
                .filter(t -> !t.isAccessTokenExpired())
                .map(t -> new TokenInfo(t.getClient().getClientId(), t.getScopes(),
                        java.time.Duration.between(LocalDateTime.now(), t.getAccessTokenExpiresAt()).getSeconds()))
                .orElse(null);
    }

    @Override
    @Transactional
    public void revokeToken(String accessToken) {
        tokenRepository.findByAccessTokenAndRevokedFalse(accessToken).ifPresent(t -> {
            t.setRevoked(true);
            tokenRepository.save(t);
        });
    }

    private String resolveScope(String requested, String allowed) {
        if (requested == null || requested.isBlank()) return allowed;
        Set<String> allowedSet = new HashSet<>(Arrays.asList(allowed.split(",")));
        String[] requestedScopes = requested.split(" ");
        StringBuilder resolved = new StringBuilder();
        for (String s : requestedScopes) {
            if (allowedSet.contains(s.trim())) {
                if (resolved.length() > 0) resolved.append(" ");
                resolved.append(s.trim());
            }
        }
        return resolved.length() > 0 ? resolved.toString() : allowed;
    }

    private String generateClientId() {
        byte[] bytes = new byte[16];
        secureRandom.nextBytes(bytes);
        return "nwl_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes).substring(0, 20);
    }

    private String generateSecret() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String generateToken() {
        byte[] bytes = new byte[48];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
