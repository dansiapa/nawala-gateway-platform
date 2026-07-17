package id.nawala.platform.service.impl;

import id.nawala.platform.logging.SecurityLogger;
import id.nawala.platform.model.AnomalyEvent;
import id.nawala.platform.model.User;
import id.nawala.platform.repository.AnomalyEventRepository;
import id.nawala.platform.repository.UserRepository;
import id.nawala.platform.service.AnomalyDetectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnomalyDetectionServiceImpl implements AnomalyDetectionService {

    private final AnomalyEventRepository anomalyEventRepository;
    private final UserRepository userRepository;

    private final Map<String, RequestWindow> ipWindows = new ConcurrentHashMap<>();
    private final Map<String, RequestWindow> keyWindows = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> failureCounters = new ConcurrentHashMap<>();

    private static final int SPIKE_THRESHOLD = 200;
    private static final int BRUTE_FORCE_THRESHOLD = 20;
    private static final int UNUSUAL_HOUR_START = 2;
    private static final int UNUSUAL_HOUR_END = 5;
    private static final long WINDOW_MS = 60_000;
    private static final int AUTO_BLOCK_MINUTES = 30;

    @Override
    public void recordRequest(String sourceIp, String apiKeyPrefix, String path, int responseStatus) {
        String ipKey = sourceIp != null ? sourceIp : "unknown";
        String keyPrefix = apiKeyPrefix != null ? apiKeyPrefix : "none";

        RequestWindow ipWindow = ipWindows.computeIfAbsent(ipKey, k -> new RequestWindow());
        ipWindow.increment();

        if (!"none".equals(keyPrefix)) {
            RequestWindow keyWindow = keyWindows.computeIfAbsent(keyPrefix, k -> new RequestWindow());
            keyWindow.increment();
        }

        // Brute force detection
        if (responseStatus == 401 || responseStatus == 403) {
            String failKey = ipKey + ":" + keyPrefix;
            AtomicInteger failures = failureCounters.computeIfAbsent(failKey, k -> new AtomicInteger(0));
            int count = failures.incrementAndGet();
            if (count >= BRUTE_FORCE_THRESHOLD) {
                createEvent("BRUTE_FORCE", "HIGH", ipKey, keyPrefix,
                        "Brute force: " + count + " auth failures/min from " + ipKey, count, path, true);
                failures.set(0);
            }
        }

        // Spike detection
        int ipCount = ipWindow.getCount();
        if (ipCount >= SPIKE_THRESHOLD && ipCount % SPIKE_THRESHOLD == 0) {
            createEvent("SPIKE", "MEDIUM", ipKey, keyPrefix,
                    "Spike: " + ipCount + " req/min from " + ipKey, ipCount, path, ipCount >= SPIKE_THRESHOLD * 3);
        }

        // Unusual hour detection
        LocalTime now = LocalTime.now();
        if (now.getHour() >= UNUSUAL_HOUR_START && now.getHour() < UNUSUAL_HOUR_END && ipWindow.getCount() > 50) {
            createEvent("UNUSUAL_HOUR", "LOW", ipKey, keyPrefix,
                    "High activity at " + now.getHour() + ":00 from " + ipKey, ipWindow.getCount(), path, false);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isBlocked(String sourceIp, String apiKeyPrefix) {
        LocalDateTime now = LocalDateTime.now();
        if (sourceIp != null) {
            List<AnomalyEvent> ipBlocks = anomalyEventRepository.findActiveBlocksByIp(sourceIp, now);
            if (!ipBlocks.isEmpty()) return true;
        }
        if (apiKeyPrefix != null) {
            List<AnomalyEvent> keyBlocks = anomalyEventRepository.findActiveBlocksByKeyPrefix(apiKeyPrefix, now);
            if (!keyBlocks.isEmpty()) return true;
        }
        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnomalyEvent> getUnresolvedEvents() {
        return anomalyEventRepository.findByResolvedFalseOrderByDetectedAtDesc();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnomalyEvent> getRecentEvents() {
        return anomalyEventRepository.findTop20ByOrderByDetectedAtDesc();
    }

    @Override
    @Transactional
    public void resolveEvent(Long eventId, Long resolvedByUserId) {
        AnomalyEvent event = anomalyEventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found"));
        User resolver = userRepository.findById(resolvedByUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        event.setResolved(true);
        event.setResolvedAt(LocalDateTime.now());
        event.setResolvedBy(resolver);
        anomalyEventRepository.save(event);
        SecurityLogger.log().info("Anomaly #{} resolved by user={}", eventId, resolver.getUsername());
    }

    @Override
    @Transactional(readOnly = true)
    public ThreatStats getThreatStats() {
        return new ThreatStats(
                anomalyEventRepository.countByResolvedFalse(),
                anomalyEventRepository.countBySeverityAndResolvedFalse("CRITICAL"),
                anomalyEventRepository.findByAutoBlockedTrueAndResolvedFalse().size(),
                anomalyEventRepository.countSince(LocalDateTime.now().minusHours(24))
        );
    }

    private void createEvent(String type, String severity, String ip, String keyPrefix,
                             String description, int count, String path, boolean autoBlock) {
        AnomalyEvent event = AnomalyEvent.builder()
                .type(type).severity(severity).sourceIp(ip).apiKeyPrefix(keyPrefix)
                .description(description).requestCount(count).targetPath(path)
                .resolved(false).autoBlocked(autoBlock)
                .blockedUntil(autoBlock ? LocalDateTime.now().plusMinutes(AUTO_BLOCK_MINUTES) : null)
                .build();
        anomalyEventRepository.save(event);
        SecurityLogger.log().warn("ANOMALY [{}] severity={} ip={} key={} path={} count={} blocked={}",
                type, severity, ip, keyPrefix, path, count, autoBlock);
    }

    @Scheduled(fixedDelay = 300000)
    public void cleanupWindows() {
        long now = System.currentTimeMillis();
        ipWindows.entrySet().removeIf(e -> now - e.getValue().windowStart > WINDOW_MS * 5);
        keyWindows.entrySet().removeIf(e -> now - e.getValue().windowStart > WINDOW_MS * 5);
        failureCounters.entrySet().removeIf(e -> e.getValue().get() == 0);
    }

    private static class RequestWindow {
        volatile long windowStart = System.currentTimeMillis();
        private final AtomicInteger count = new AtomicInteger(0);

        void increment() {
            long now = System.currentTimeMillis();
            if (now - windowStart > WINDOW_MS) { windowStart = now; count.set(0); }
            count.incrementAndGet();
        }

        int getCount() {
            long now = System.currentTimeMillis();
            if (now - windowStart > WINDOW_MS) return 0;
            return count.get();
        }
    }
}
