package id.nawala.platform.repository;

import id.nawala.platform.model.ApiRoute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApiRouteRepository extends JpaRepository<ApiRoute, Long> {

    List<ApiRoute> findByActiveTrue();

    List<ApiRoute> findByCreatedById(Long userId);

    Optional<ApiRoute> findByPathAndMethod(String path, String method);

    boolean existsByPathAndMethod(String path, String method);

    long countByActive(boolean active);
}
