package id.nawala.platform.service.impl;

import id.nawala.platform.model.ApiRoute;
import id.nawala.platform.repository.ApiRouteRepository;
import id.nawala.platform.service.HealthMonitorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@EnableScheduling
public class HealthMonitorServiceImpl implements HealthMonitorService {

    private final ApiRouteRepository apiRouteRepository;

    private static final int TIMEOUT_MS = 5000;
    private static final int DEGRADED_THRESHOLD_MS = 3000;

    @Override
    @Scheduled(fixedDelay = 60000, initialDelay = 10000)
    @Transactional
    public void checkAllRoutes() {
        List<ApiRoute> routes = apiRouteRepository.findByActiveTrue();

        for (ApiRoute route : routes) {
            if (route.getHealthCheckUrl() == null || route.getHealthCheckUrl().isBlank()) {
                continue;
            }

            long start = System.currentTimeMillis();
            String status;
            int responseTime;

            try {
                URL url = new URL(route.getHealthCheckUrl());
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(TIMEOUT_MS);
                conn.setReadTimeout(TIMEOUT_MS);
                conn.setInstanceFollowRedirects(true);

                int code = conn.getResponseCode();
                responseTime = (int) (System.currentTimeMillis() - start);
                conn.disconnect();

                if (code >= 200 && code < 300) {
                    status = responseTime > DEGRADED_THRESHOLD_MS ? "DEGRADED" : "UP";
                } else if (code >= 500) {
                    status = "DOWN";
                } else {
                    status = "DEGRADED";
                }
            } catch (Exception e) {
                responseTime = (int) (System.currentTimeMillis() - start);
                status = "DOWN";
                log.debug("Health check failed for {}: {}", route.getName(), e.getMessage());
            }

            route.setHealthStatus(status);
            route.setLastHealthCheck(LocalDateTime.now());
            route.setLastResponseTimeMs(responseTime);
            apiRouteRepository.save(route);

            if ("DOWN".equals(status)) {
                log.warn("Service DOWN: {} ({})", route.getName(), route.getHealthCheckUrl());
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public HealthSummary getHealthSummary() {
        List<ApiRoute> routes = apiRouteRepository.findByActiveTrue();
        List<ApiRoute> monitored = routes.stream()
                .filter(r -> r.getHealthCheckUrl() != null && !r.getHealthCheckUrl().isBlank())
                .collect(Collectors.toList());

        int up = 0, down = 0, degraded = 0, unknown = 0;
        for (ApiRoute r : monitored) {
            switch (r.getHealthStatus() != null ? r.getHealthStatus() : "UNKNOWN") {
                case "UP" -> up++;
                case "DOWN" -> down++;
                case "DEGRADED" -> degraded++;
                default -> unknown++;
            }
        }

        return new HealthSummary(monitored.size(), up, down, degraded, unknown);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RouteHealth> getAllRouteHealth() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        return apiRouteRepository.findByActiveTrue().stream()
                .filter(r -> r.getHealthCheckUrl() != null && !r.getHealthCheckUrl().isBlank())
                .map(r -> new RouteHealth(
                        r.getId(),
                        r.getName(),
                        r.getPath(),
                        r.getHealthStatus() != null ? r.getHealthStatus() : "UNKNOWN",
                        r.getLastResponseTimeMs(),
                        r.getLastHealthCheck() != null ? r.getLastHealthCheck().format(formatter) : "Never"
                ))
                .collect(Collectors.toList());
    }
}
