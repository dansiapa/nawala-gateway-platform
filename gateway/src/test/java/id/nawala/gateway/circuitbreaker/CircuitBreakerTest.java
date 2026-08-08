package id.nawala.gateway.circuitbreaker;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CircuitBreakerTest {
    
    private CircuitBreaker circuitBreaker;
    
    @BeforeEach
    void setUp() {
        circuitBreaker = new CircuitBreaker();
    }
    
    @Test
    void testInitialState_Closed() {
        assertEquals(CircuitBreaker.Status.CLOSED, circuitBreaker.getStatus("test-target"));
    }
    
    @Test
    void testAllowRequest_WhenClosed() {
        assertTrue(circuitBreaker.allowRequest("test-target"));
    }
    
    @Test
    void testCircuitOpens_AfterFailureThreshold() {
        String target = "failing-target";
        
        for (int i = 0; i < 5; i++) {
            circuitBreaker.allowRequest(target);
            circuitBreaker.recordFailure(target);
        }
        
        assertEquals(CircuitBreaker.Status.OPEN, circuitBreaker.getStatus(target));
        assertFalse(circuitBreaker.allowRequest(target));
    }
    
    @Test
    void testCircuitResets_OnSuccess() {
        String target = "recovering-target";
        
        for (int i = 0; i < 3; i++) {
            circuitBreaker.recordFailure(target);
        }
        
        circuitBreaker.recordSuccess(target);
        assertEquals(CircuitBreaker.Status.CLOSED, circuitBreaker.getStatus(target));
    }
    
    @Test
    void testReset() {
        String target = "reset-target";
        
        for (int i = 0; i < 5; i++) {
            circuitBreaker.recordFailure(target);
        }
        assertEquals(CircuitBreaker.Status.OPEN, circuitBreaker.getStatus(target));
        
        circuitBreaker.reset(target);
        
        assertEquals(CircuitBreaker.Status.CLOSED, circuitBreaker.getStatus(target));
        assertTrue(circuitBreaker.allowRequest(target));
    }
    
    @Test
    void testDifferentTargets_IndependentCircuits() {
        for (int i = 0; i < 5; i++) {
            circuitBreaker.recordFailure("target1");
        }
        
        assertEquals(CircuitBreaker.Status.OPEN, circuitBreaker.getStatus("target1"));
        assertEquals(CircuitBreaker.Status.CLOSED, circuitBreaker.getStatus("target2"));
        assertTrue(circuitBreaker.allowRequest("target2"));
    }
}
