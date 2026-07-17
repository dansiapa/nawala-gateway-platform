package id.nawala.platform.repository;

import id.nawala.platform.model.RouteTransformation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RouteTransformationRepository extends JpaRepository<RouteTransformation, Long> {

    List<RouteTransformation> findByRouteIdAndPhaseAndActiveTrueOrderByPriorityAsc(Long routeId, String phase);

    List<RouteTransformation> findByRouteId(Long routeId);
}
