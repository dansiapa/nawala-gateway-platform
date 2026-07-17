package id.nawala.platform.service;

import id.nawala.platform.model.Webhook;
import java.util.List;

/**
 * Webhook & Event notification service.
 */
public interface WebhookService {

    Webhook create(Long userId, String name, String targetUrl, String eventType, String secret);

    List<Webhook> getByUser(Long userId);

    void delete(Long webhookId);

    void toggle(Long webhookId, boolean active);

    void fireEvent(String eventType, String payload);

    List<WebhookDeliveryInfo> getDeliveries(Long webhookId);

    record WebhookDeliveryInfo(
            Long id,
            String eventType,
            int httpStatus,
            String status,
            int attemptNumber,
            long durationMs,
            String deliveredAt
    ) {}
}
