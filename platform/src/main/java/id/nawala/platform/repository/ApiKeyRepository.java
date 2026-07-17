package id.nawala.platform.repository;

import id.nawala.platform.model.ApiKey;
import id.nawala.platform.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {

    List<ApiKey> findByOwner(User owner);

    Optional<ApiKey> findByKeyHash(String keyHash);

    Optional<ApiKey> findByPrefix(String prefix);

    long countByActiveTrue();
}
