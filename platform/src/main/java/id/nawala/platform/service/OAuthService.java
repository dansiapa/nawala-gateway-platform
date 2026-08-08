package id.nawala.platform.service;

import id.nawala.platform.model.OAuthClient;
import id.nawala.platform.model.User;

import java.util.List;
import java.util.Map;

/**
 * OAuth2 Authorization Server service.
 * Supports client_credentials and refresh_token grant types.
 */
public interface OAuthService {

    OAuthClient registerClient(Long userId, String name, String grantTypes, String scopes, String redirectUris);
    
    OAuthClient createClient(User owner, String name, String redirectUri, List<String> scopes);

    List<OAuthClient> getClientsByUser(Long userId);

    void deleteClient(Long clientId);
    
    String regenerateSecret(Long clientId);

    /**
     * Issue token via client_credentials grant.
     */
    Map<String, Object> issueToken(String clientId, String clientSecret, String grantType, String scope);

    /**
     * Refresh an existing token.
     */
    Map<String, Object> refreshToken(String refreshToken);

    /**
     * Validate an access token.
     */
    TokenInfo validateToken(String accessToken);

    void revokeToken(String accessToken);

    record TokenInfo(String clientId, String scopes, long expiresInSeconds) {}
}
