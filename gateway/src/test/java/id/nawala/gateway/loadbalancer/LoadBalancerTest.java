package id.nawala.gateway.loadbalancer;

import id.nawala.gateway.core.RouteDefinition;
import id.nawala.gateway.core.TargetDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LoadBalancerTest {
    
    private LoadBalancer loadBalancer;
    
    @BeforeEach
    void setUp() {
        loadBalancer = new LoadBalancer();
    }
    
    @Test
    void testSelectTarget_NotLoadBalanced() {
        RouteDefinition route = RouteDefinition.builder()
            .id(1L)
            .targetUrl("http://primary.example.com")
            .loadBalanced(false)
            .build();
        
        String target = loadBalancer.selectTarget(route);
        assertEquals("http://primary.example.com", target);
    }
    
    @Test
    void testSelectTarget_RoundRobin() {
        List<TargetDefinition> targets = List.of(
            TargetDefinition.builder().id(1L).url("http://server1.com").healthy(true).build(),
            TargetDefinition.builder().id(2L).url("http://server2.com").healthy(true).build(),
            TargetDefinition.builder().id(3L).url("http://server3.com").healthy(true).build()
        );
        
        RouteDefinition route = RouteDefinition.builder()
            .id(1L)
            .targetUrl("http://primary.com")
            .loadBalanced(true)
            .lbStrategy("ROUND_ROBIN")
            .targets(targets)
            .build();
        
        Map<String, Integer> counts = new HashMap<>();
        for (int i = 0; i < 9; i++) {
            String target = loadBalancer.selectTarget(route);
            counts.merge(target, 1, Integer::sum);
        }
        
        assertEquals(3, counts.getOrDefault("http://server1.com", 0));
        assertEquals(3, counts.getOrDefault("http://server2.com", 0));
        assertEquals(3, counts.getOrDefault("http://server3.com", 0));
    }
    
    @Test
    void testSelectTarget_OnlyHealthyTargets() {
        List<TargetDefinition> targets = List.of(
            TargetDefinition.builder().id(1L).url("http://healthy.com").healthy(true).build(),
            TargetDefinition.builder().id(2L).url("http://unhealthy.com").healthy(false).build()
        );
        
        RouteDefinition route = RouteDefinition.builder()
            .id(1L)
            .targetUrl("http://primary.com")
            .loadBalanced(true)
            .lbStrategy("ROUND_ROBIN")
            .targets(targets)
            .build();
        
        for (int i = 0; i < 10; i++) {
            assertEquals("http://healthy.com", loadBalancer.selectTarget(route));
        }
    }
    
    @Test
    void testSelectTarget_NoHealthyTargets_FallbackToPrimary() {
        List<TargetDefinition> targets = List.of(
            TargetDefinition.builder().id(1L).url("http://unhealthy1.com").healthy(false).build(),
            TargetDefinition.builder().id(2L).url("http://unhealthy2.com").healthy(false).build()
        );
        
        RouteDefinition route = RouteDefinition.builder()
            .id(1L)
            .targetUrl("http://primary.com")
            .loadBalanced(true)
            .lbStrategy("ROUND_ROBIN")
            .targets(targets)
            .build();
        
        assertEquals("http://primary.com", loadBalancer.selectTarget(route));
    }
}
