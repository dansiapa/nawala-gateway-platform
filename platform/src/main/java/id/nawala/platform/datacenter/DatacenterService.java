package id.nawala.platform.datacenter;

import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DatacenterService {

    private final DatacenterRepository datacenterRepository;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public List<DatacenterConfig> getAllDatacenters() {
        return datacenterRepository.findAll();
    }

    public List<DatacenterConfig> getActiveDatacenters() {
        return datacenterRepository.findByEnabledTrueOrderByWeightDesc();
    }

    public Optional<DatacenterConfig> getPrimaryDatacenter() {
        return datacenterRepository.findByPrimaryTrue();
    }

    public DatacenterConfig createDatacenter(DatacenterConfig dc) {
        if (dc.isPrimary()) {
            datacenterRepository.findByPrimaryTrue().ifPresent(existing -> {
                existing.setPrimary(false);
                datacenterRepository.save(existing);
            });
        }
        return datacenterRepository.save(dc);
    }

    public void deleteDatacenter(Long id) {
        datacenterRepository.deleteById(id);
    }

    public DatacenterConfig selectDatacenter() {
        List<DatacenterConfig> active = getActiveDatacenters().stream()
                .filter(dc -> dc.getStatus() == DatacenterConfig.DatacenterStatus.HEALTHY)
                .toList();
        
        if (active.isEmpty()) {
            return getPrimaryDatacenter().orElseThrow(() -> 
                new RuntimeException("No healthy datacenters available"));
        }
        
        // Weighted random selection
        int totalWeight = active.stream().mapToInt(DatacenterConfig::getWeight).sum();
        int random = (int) (Math.random() * totalWeight);
        int cumulative = 0;
        
        for (DatacenterConfig dc : active) {
            cumulative += dc.getWeight();
            if (random < cumulative) {
                return dc;
            }
        }
        
        return active.get(0);
    }

    @Scheduled(fixedRate = 30000) // Every 30 seconds
    public void healthCheck() {
        getAllDatacenters().forEach(this::checkHealth);
    }

    private void checkHealth(DatacenterConfig dc) {
        if (!dc.isEnabled()) return;
        
        try {
            long start = System.currentTimeMillis();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(dc.getEndpoint() + dc.getHealthEndpoint()))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, 
                    HttpResponse.BodyHandlers.ofString());
            
            long latency = System.currentTimeMillis() - start;
            dc.setLatencyMs(latency);
            dc.setLastHealthCheck(LocalDateTime.now());
            
            if (response.statusCode() == 200) {
                dc.setStatus(DatacenterConfig.DatacenterStatus.HEALTHY);
            } else {
                dc.setStatus(DatacenterConfig.DatacenterStatus.DEGRADED);
            }
        } catch (Exception e) {
            dc.setStatus(DatacenterConfig.DatacenterStatus.UNHEALTHY);
            log.warn("Health check failed for {}: {}", dc.getName(), e.getMessage());
        }
        
        datacenterRepository.save(dc);
    }
}
