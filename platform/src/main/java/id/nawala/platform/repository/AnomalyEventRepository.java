package id.nawala.platform.repository;

import id.nawala.platform.model.AnomalyEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AnomalyEventRepository extends JpaRepository<AnomalyEvent, Long> {

    List<AnomalyEvent> findByResolvedFalseOrderByDetectedAtDesc();

    List<AnomalyEvent> findByAutoBlockedTrueAndResolvedFalse();

    List<AnomalyEvent> findTop20ByOrderByDetectedAtDesc();

    @Query("SELECT a FROM AnomalyEvent a WHERE a.sourceIp = :ip AND a.autoBlocked = true AND a.resolved = false AND a.blockedUntil > :now")
    List<AnomalyEvent> findActiveBlocksByIp(String ip, LocalDateTime now);

    @Query("SELECT a FROM AnomalyEvent a WHERE a.apiKeyPrefix = :prefix AND a.autoBlocked = true AND a.resolved = false AND a.blockedUntil > :now")
    List<AnomalyEvent> findActiveBlocksByKeyPrefix(String prefix, LocalDateTime now);

    long countByResolvedFalse();

    long countBySeverityAndResolvedFalse(String severity);

    @Query("SELECT COUNT(a) FROM AnomalyEvent a WHERE a.detectedAt > :since")
    long countSince(LocalDateTime since);
}
