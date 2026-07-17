package id.nawala.platform.service.impl;

import id.nawala.platform.logging.SecurityLogger;
import id.nawala.platform.model.Webhook;
import id.nawala.platform.model.WebhookDelivery;
import id.nawala.platform.model.User;
import id.nawala.platform.repository.UserRepository;
import id.nawala.platform.repository.WebhookDeliveryRepository;
import id.nawala.platform.repository.WebhookRepository;
import id.nawala.platform.service.WebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookServiceImpl implements WebhookService {

    private final WebhookRepository webhookRepository;
    private final WebhookDeliveryRepository deliveryRepository;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    @Transactional
    public Webhook create(Long userId, String name, String targetUrl, String eventType, String secret) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Webhook webhook = Webhook.builder()
                .owner(user).name(name).targetUrl(targetUrl)
                .eventType(eventType).secret(secret).active(true)
                .build();
        return webhookRepository.save(webhook);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Webhook> getByUser(Long userId) {
        return webhookRepository.findByOwnerId(userId);
    }

    @Override
    @Transactional
    public void delete(Long webhookId) {
        webhookRepository.deleteById(webhookId);
    }

    @Override
    @Transactional
    public void toggle(Long webhookId, boolean active) {
        webhookRepository.findById(webhookId).ifPresent(w -> {
            w.setActive(active);
            webhookRepository.save(w);
        });
    }

    @Override
    @Async
    public void fireEvent(String eventType, String payload) {
        List<Webhook> webhooks = webhookRepository.findByEventTypeAndActiveTrue(eventType);
        for (Webhook webhook : webhooks) {
            deliver(webhook, eventType, payload, 1);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<WebhookDeliveryInfo> getDeliveries(Long webhookId) {
        return deliveryRepository.findByWebhookIdOrderByDeliveredAtDesc(webhookId).stream()
                .limit(50)
                .map(d -> new WebhookDeliveryInfo(
                        d.getId(), d.getEventType(), d.getHttpStatus(),
                        d.getStatus(), d.getAttemptNumber(), d.getDurationMs(),
                        d.getDeliveredAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                ))
                .collect(Collectors.toList());
    }

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void retryFailedDeliveries() {
        List<WebhookDelivery> pending = deliveryRepository
                .findByStatusAndNextRetryAtBefore("RETRYING", LocalDateTime.now());
        for (WebhookDelivery delivery : pending) {
            Webhook webhook = delivery.getWebhook();
            if (delivery.getAttemptNumber() >= webhook.getMaxRetries()) {
                delivery.setStatus("FAILED");
                deliveryRepository.save(delivery);
                continue;
            }
            deliver(webhook, delivery.getEventType(), delivery.getPayload(), delivery.getAttemptNumber() + 1);
        }
    }

    private void deliver(Webhook webhook, String eventType, String payload, int attempt) {
        long start = System.currentTimeMillis();
        WebhookDelivery delivery = WebhookDelivery.builder()
                .webhook(webhook).eventType(eventType).payload(payload)
                .attemptNumber(attempt).status("PENDING")
                .build();
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Nawala-Event", eventType);
            headers.set("X-Nawala-Delivery-Attempt", String.valueOf(attempt));
            if (webhook.getSecret() != null && !webhook.getSecret().isBlank()) {
                headers.set("X-Nawala-Signature", computeHmac(payload, webhook.getSecret()));
            }
            HttpEntity<String> entity = new HttpEntity<>(payload, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    webhook.getTargetUrl(), HttpMethod.POST, entity, String.class);
            long duration = System.currentTimeMillis() - start;
            delivery.setHttpStatus(response.getStatusCode().value());
            delivery.setDurationMs(duration);
            delivery.setStatus("SUCCESS");
            delivery.setResponseBody(truncate(response.getBody(), 500));
            webhook.setLastTriggeredAt(LocalDateTime.now());
            webhook.setLastStatus("SUCCESS");
            webhookRepository.save(webhook);
            SecurityLogger.log().info("Webhook delivered name={} event={} status={}",
                    webhook.getName(), eventType, response.getStatusCode().value());
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            delivery.setDurationMs(duration);
            delivery.setStatus("RETRYING");
            delivery.setResponseBody(truncate(e.getMessage(), 500));
            long delayMinutes = (long) Math.pow(4, attempt - 1);
            delivery.setNextRetryAt(LocalDateTime.now().plusMinutes(delayMinutes));
            webhook.setLastTriggeredAt(LocalDateTime.now());
            webhook.setLastStatus("FAILED");
            webhookRepository.save(webhook);
            log.warn("Webhook delivery failed name={} attempt={} err={}", webhook.getName(), attempt, e.getMessage());
        }
        deliveryRepository.save(delivery);
    }

    private String computeHmac(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return "sha256=" + Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) { return ""; }
    }

    private String truncate(String str, int maxLen) {
        if (str == null) return null;
        return str.length() > maxLen ? str.substring(0, maxLen) : str;
    }
}
