package id.nawala.platform.repository;

import id.nawala.platform.model.ApiRoute;
import id.nawala.platform.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApiRouteRepository extends JpaRepository<ApiRoute, Long> {

    List<ApiRoute> findByActiveTrue();

    List<ApiRoute> findByCreatedById(Long userId);
    
    List<ApiRoute> findByCreatedBy(User user);

    Optional<ApiRoute> findByPathAndMethod(String path, String method);

    boolean existsByPathAndMethod(String path, String method);

    long countByActive(boolean active);
    
    long countByCreatedBy(User user);
    
    @Query("SELECT COUNT(r) FROM ApiRoute r WHERE r.healthStatus = :status")
    long countByHealthStatus(String status);
    
    List<ApiRoute> findByHealthCheckUrlIsNotNull();
}
