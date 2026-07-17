package id.nawala.platform.repository;

import id.nawala.platform.model.Webhook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WebhookRepository extends JpaRepository<Webhook, Long> {

    List<Webhook> findByOwnerIdAndActiveTrue(Long userId);

    List<Webhook> findByEventTypeAndActiveTrue(String eventType);

    List<Webhook> findByOwnerId(Long userId);
}
