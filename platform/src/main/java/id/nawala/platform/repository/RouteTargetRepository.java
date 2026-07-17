package id.nawala.platform.repository;

import id.nawala.platform.model.RouteTarget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RouteTargetRepository extends JpaRepository<RouteTarget, Long> {

    List<RouteTarget> findByRouteIdAndActiveTrue(Long routeId);

    List<RouteTarget> findByRouteIdAndActiveTrueAndHealthyTrue(Long routeId);

    List<RouteTarget> findByRouteId(Long routeId);
}
