package id.nawala.platform.service.impl;

import id.nawala.platform.model.ApiAnalytics;
import id.nawala.platform.repository.ApiAnalyticsRepository;
import id.nawala.platform.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsServiceImpl implements AnalyticsService {

    private final ApiAnalyticsRepository analyticsRepository;

    @Override
    @Async
    public void recordRequest(Long routeId, String apiKeyPrefix, String sourceIp,
                              String method, String path, int statusCode,
                              long responseTimeMs, long requestSize, long responseSize) {
        ApiAnalytics analytics = ApiAnalytics.builder()
                .routeId(routeId)
                .apiKeyPrefix(apiKeyPrefix)
                .sourceIp(sourceIp)
                .method(method)
                .path(path)
                .statusCode(statusCode)
                .responseTimeMs(responseTimeMs)
                .requestSizeBytes(requestSize)
                .responseSizeBytes(responseSize)
                .build();
        analyticsRepository.save(analytics);
    }

    @Override
    @Transactional(readOnly = true)
    public AnalyticsSummary getSummary(LocalDateTime since) {
        long total = analyticsRepository.countSince(since);
        Double avgTime = analyticsRepository.avgResponseTimeSince(since);
        long errors = analyticsRepository.countErrorsSince(since);
        double errorRate = total > 0 ? (double) errors / total * 100 : 0;
        return new AnalyticsSummary(total, avgTime != null ? avgTime : 0, errors, errorRate, 0);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RouteAnalytics> getTopRoutes(LocalDateTime since, int limit) {
        return analyticsRepository.getTopRoutesSince(since).stream()
                .limit(limit)
                .map(row -> new RouteAnalytics(
                        (String) row[0],
                        (Long) row[1],
                        row[2] != null ? (Double) row[2] : 0
                ))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<KeyAnalytics> getTopKeys(LocalDateTime since, int limit) {
        return analyticsRepository.getTopKeysSince(since).stream()
                .limit(limit)
                .map(row -> new KeyAnalytics((String) row[0], (Long) row[1]))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Integer, Long> getStatusDistribution(LocalDateTime since) {
        Map<Integer, Long> result = new LinkedHashMap<>();
        analyticsRepository.getStatusDistribution(since)
                .forEach(row -> result.put((Integer) row[0], (Long) row[1]));
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Integer, Long> getHourlyTraffic(LocalDateTime since) {
        Map<Integer, Long> result = new LinkedHashMap<>();
        analyticsRepository.getHourlyTraffic(since)
                .forEach(row -> result.put((Integer) row[0], (Long) row[1]));
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> getGeoDistribution(LocalDateTime since) {
        Map<String, Long> result = new LinkedHashMap<>();
        analyticsRepository.getGeoDistribution(since)
                .forEach(row -> result.put((String) row[0], (Long) row[1]));
        return result;
    }
}
