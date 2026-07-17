package id.nawala.platform.repository;

import id.nawala.platform.model.ApiDoc;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApiDocRepository extends JpaRepository<ApiDoc, Long> {

    List<ApiDoc> findByOwnerId(Long ownerId);

    List<ApiDoc> findByPublishedTrue();

    Optional<ApiDoc> findByRouteId(Long routeId);
}
