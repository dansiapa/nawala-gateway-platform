package id.nawala.platform.repository;

import id.nawala.platform.model.WafRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WafRuleRepository extends JpaRepository<WafRule, Long> {

    List<WafRule> findByActiveTrueOrderByPriorityAsc();

    List<WafRule> findByRouteIdAndActiveTrueOrderByPriorityAsc(Long routeId);

    List<WafRule> findByRouteIdIsNullAndActiveTrueOrderByPriorityAsc();
}
