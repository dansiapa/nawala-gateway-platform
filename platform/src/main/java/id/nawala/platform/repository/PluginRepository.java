package id.nawala.platform.repository;

import id.nawala.platform.model.Plugin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PluginRepository extends JpaRepository<Plugin, Long> {

    List<Plugin> findByOwnerId(Long ownerId);

    List<Plugin> findByActiveTrueOrderByPriorityAsc();

    List<Plugin> findByHookTypeAndActiveTrueOrderByPriorityAsc(String hookType);

    List<Plugin> findByRouteIdAndHookTypeAndActiveTrueOrderByPriorityAsc(Long routeId, String hookType);
}
