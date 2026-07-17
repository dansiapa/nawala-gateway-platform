package id.nawala.platform.repository;

import id.nawala.platform.model.OAuthClient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OAuthClientRepository extends JpaRepository<OAuthClient, Long> {

    Optional<OAuthClient> findByClientIdAndActiveTrue(String clientId);

    Optional<OAuthClient> findByClientId(String clientId);

    List<OAuthClient> findByOwnerId(Long ownerId);
}
