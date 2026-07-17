package id.nawala.platform.controller;

import id.nawala.platform.service.OAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * OAuth2 Token endpoint.
 * POST /oauth/token - issue or refresh tokens
 * POST /oauth/revoke - revoke token
 * GET  /oauth/introspect - validate token
 */
@RestController
@RequestMapping("/oauth")
@RequiredArgsConstructor
public class OAuthController {

    private final OAuthService oAuthService;

    @PostMapping(value = "/token", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> token(@RequestParam("grant_type") String grantType,
                                   @RequestParam(value = "client_id", required = false) String clientId,
                                   @RequestParam(value = "client_secret", required = false) String clientSecret,
                                   @RequestParam(value = "scope", required = false) String scope,
                                   @RequestParam(value = "refresh_token", required = false) String refreshToken) {
        try {
            Map<String, Object> result;
            if ("refresh_token".equals(grantType)) {
                if (refreshToken == null) {
                    return badRequest("refresh_token parameter required");
                }
                result = oAuthService.refreshToken(refreshToken);
            } else {
                if (clientId == null || clientSecret == null) {
                    return badRequest("client_id and client_secret required");
                }
                result = oAuthService.issueToken(clientId, clientSecret, grantType, scope);
            }
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "invalid_client", "error_description", e.getMessage()));
        }
    }

    @PostMapping("/revoke")
    public ResponseEntity<?> revoke(@RequestParam("token") String token) {
        oAuthService.revokeToken(token);
        return ResponseEntity.ok(Map.of("status", "revoked"));
    }

    @GetMapping("/introspect")
    public ResponseEntity<?> introspect(@RequestParam("token") String token) {
        OAuthService.TokenInfo info = oAuthService.validateToken(token);
        if (info == null) {
            return ResponseEntity.ok(Map.of("active", false));
        }
        return ResponseEntity.ok(Map.of(
                "active", true,
                "client_id", info.clientId(),
                "scope", info.scopes(),
                "expires_in", info.expiresInSeconds()
        ));
    }

    private ResponseEntity<?> badRequest(String msg) {
        return ResponseEntity.badRequest()
                .body(Map.of("error", "invalid_request", "error_description", msg));
    }
}
