package id.nawala.platform.repository;

import id.nawala.platform.model.OAuthToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OAuthTokenRepository extends JpaRepository<OAuthToken, Long> {

    Optional<OAuthToken> findByAccessTokenAndRevokedFalse(String accessToken);

    Optional<OAuthToken> findByRefreshTokenAndRevokedFalse(String refreshToken);

    void deleteByClientId(Long clientId);
}
