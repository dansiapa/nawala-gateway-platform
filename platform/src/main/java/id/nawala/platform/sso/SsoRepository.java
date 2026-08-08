package id.nawala.platform.sso;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SsoRepository extends JpaRepository<SsoConfig, Long> {
    Optional<SsoConfig> findByIsDefaultTrue();
    List<SsoConfig> findByEnabledTrue();
    Optional<SsoConfig> findByName(String name);
    List<SsoConfig> findByType(SsoConfig.SsoType type);
}
