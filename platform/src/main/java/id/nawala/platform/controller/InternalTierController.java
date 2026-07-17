package id.nawala.platform.controller;

import id.nawala.platform.repository.RateLimitTierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Internal endpoint for gateway to fetch rate limit tiers.
 */
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalTierController {

    private final RateLimitTierRepository rateLimitTierRepository;

    @GetMapping("/rate-tiers")
    public ResponseEntity<List<Map<String, Object>>> getRateTiers() {
        List<Map<String, Object>> tiers = rateLimitTierRepository.findByActiveTrue().stream()
                .map(t -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("name", t.getName());
                    m.put("requestsPerMinute", t.getRequestsPerMinute());
                    m.put("requestsPerHour", t.getRequestsPerHour());
                    m.put("requestsPerDay", t.getRequestsPerDay());
                    m.put("burstSize", t.getBurstSize());
                    m.put("description", t.getDescription());
                    return m;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(tiers);
    }
}
