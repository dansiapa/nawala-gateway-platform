package id.nawala.platform.service;

import id.nawala.platform.model.OAuthClient;

import java.util.List;
import java.util.Map;

/**
 * OAuth2 Authorization Server service.
 * Supports client_credentials and refresh_token grant types.
 */
public interface OAuthService {

    OAuthClient registerClient(Long userId, String name, String grantTypes, String scopes, String redirectUris);

    List<OAuthClient> getClientsByUser(Long userId);

    void deleteClient(Long clientId);

    /**
     * Issue token via client_credentials grant.
     * Returns map with access_token, token_type, expires_in, scope, refresh_token
     */
    Map<String, Object> issueToken(String clientId, String clientSecret, String grantType, String scope);

    /**
     * Refresh an existing token.
     */
    Map<String, Object> refreshToken(String refreshToken);

    /**
     * Validate an access token.
     * Returns null if invalid/expired, otherwise returns token info.
     */
    TokenInfo validateToken(String accessToken);

    void revokeToken(String accessToken);

    record TokenInfo(String clientId, String scopes, long expiresInSeconds) {}
}
