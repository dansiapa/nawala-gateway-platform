package id.nawala.platform.repository;

import id.nawala.platform.model.WebhookDelivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface WebhookDeliveryRepository extends JpaRepository<WebhookDelivery, Long> {

    List<WebhookDelivery> findByWebhookIdOrderByDeliveredAtDesc(Long webhookId);

    List<WebhookDelivery> findByStatusAndNextRetryAtBefore(String status, LocalDateTime before);

    long countByWebhookIdAndStatus(Long webhookId, String status);
}
