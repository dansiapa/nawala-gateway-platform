package id.nawala.platform.repository;

import id.nawala.platform.model.RateLimitTier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RateLimitTierRepository extends JpaRepository<RateLimitTier, Long> {

    Optional<RateLimitTier> findByName(String name);

    List<RateLimitTier> findByActiveTrue();
}
