package id.nawala.gateway.loadbalancer;

import id.nawala.gateway.core.RouteDefinition;
import id.nawala.gateway.core.TargetDefinition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Random;

@Component
@Slf4j
public class LoadBalancer {
    
    private final Map<Long, AtomicInteger> roundRobinCounters = new ConcurrentHashMap<>();
    private final Map<Long, AtomicLong> leastConnections = new ConcurrentHashMap<>();
    private final Random random = new Random();
    
    public String selectTarget(RouteDefinition route) {
        if (!route.isLoadBalanced() || route.getTargets() == null || route.getTargets().isEmpty()) {
            return route.getTargetUrl();
        }
        
        List<TargetDefinition> healthyTargets = route.getTargets().stream()
                .filter(TargetDefinition::isHealthy)
                .toList();
        
        if (healthyTargets.isEmpty()) {
            log.warn("No healthy targets for route {}, using primary", route.getName());
            return route.getTargetUrl();
        }
        
        String strategy = route.getLbStrategy() != null ? route.getLbStrategy() : "ROUND_ROBIN";
        
        return switch (strategy.toUpperCase()) {
            case "ROUND_ROBIN" -> roundRobin(route.getId(), healthyTargets);
            case "RANDOM" -> randomSelect(healthyTargets);
            case "WEIGHTED" -> weightedSelect(healthyTargets);
            case "LEAST_CONNECTIONS" -> leastConnectionsSelect(route.getId(), healthyTargets);
            case "IP_HASH" -> ipHashSelect(healthyTargets, Thread.currentThread().getName()); // Simplified
            default -> roundRobin(route.getId(), healthyTargets);
        };
    }
    
    private String roundRobin(Long routeId, List<TargetDefinition> targets) {
        AtomicInteger counter = roundRobinCounters.computeIfAbsent(routeId, k -> new AtomicInteger(0));
        int index = Math.abs(counter.getAndIncrement() % targets.size());
        return targets.get(index).getUrl();
    }
    
    private String randomSelect(List<TargetDefinition> targets) {
        return targets.get(random.nextInt(targets.size())).getUrl();
    }
    
    private String weightedSelect(List<TargetDefinition> targets) {
        int totalWeight = targets.stream().mapToInt(TargetDefinition::getWeight).sum();
        if (totalWeight == 0) {
            return randomSelect(targets);
        }
        
        int randomWeight = random.nextInt(totalWeight);
        int currentWeight = 0;
        
        for (TargetDefinition target : targets) {
            currentWeight += target.getWeight();
            if (randomWeight < currentWeight) {
                return target.getUrl();
            }
        }
        
        return targets.get(0).getUrl();
    }
    
    private String leastConnectionsSelect(Long routeId, List<TargetDefinition> targets) {
        // Simplified - in production would track actual connections
        return roundRobin(routeId, targets);
    }
    
    private String ipHashSelect(List<TargetDefinition> targets, String key) {
        int hash = Math.abs(key.hashCode());
        int index = hash % targets.size();
        return targets.get(index).getUrl();
    }
    
    public void recordConnection(Long routeId, String targetUrl) {
        // Track for least connections algorithm
    }
    
    public void releaseConnection(Long routeId, String targetUrl) {
        // Release for least connections algorithm
    }
}
