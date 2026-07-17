package id.nawala.platform.repository;

import id.nawala.platform.model.ActivityLog;
import id.nawala.platform.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    List<ActivityLog> findTop10ByUserOrderByCreatedAtDesc(User user);

    List<ActivityLog> findTop20ByOrderByCreatedAtDesc();
}
