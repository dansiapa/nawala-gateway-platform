package id.nawala.gateway.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RateLimiterTest {
    
    private RateLimiter rateLimiter;
    
    @BeforeEach
    void setUp() {
        rateLimiter = new RateLimiter();
    }
    
    @Test
    void testTryAcquire_WithinLimit() {
        String clientKey = "test-client";
        Long routeId = 1L;
        int limit = 10;
        
        // Should allow requests within limit
        for (int i = 0; i < limit; i++) {
            assertTrue(rateLimiter.tryAcquire(clientKey, routeId, limit), 
                "Request " + (i+1) + " should be allowed");
        }
    }
    
    @Test
    void testTryAcquire_ExceedsLimit() {
        String clientKey = "test-client";
        Long routeId = 1L;
        int limit = 5;
        
        // Exhaust the limit
        for (int i = 0; i < limit; i++) {
            rateLimiter.tryAcquire(clientKey, routeId, limit);
        }
        
        // Next request should be denied
        assertFalse(rateLimiter.tryAcquire(clientKey, routeId, limit));
    }
    
    @Test
    void testTryAcquire_NoLimit() {
        String clientKey = "test-client";
        Long routeId = 1L;
        
        // With 0 limit, should always allow
        for (int i = 0; i < 100; i++) {
            assertTrue(rateLimiter.tryAcquire(clientKey, routeId, 0));
        }
    }
    
    @Test
    void testTryAcquire_DifferentClients() {
        Long routeId = 1L;
        int limit = 3;
        
        // Each client has their own bucket
        for (int i = 0; i < limit; i++) {
            assertTrue(rateLimiter.tryAcquire("client1", routeId, limit));
            assertTrue(rateLimiter.tryAcquire("client2", routeId, limit));
        }
        
        // Both should be exhausted now
        assertFalse(rateLimiter.tryAcquire("client1", routeId, limit));
        assertFalse(rateLimiter.tryAcquire("client2", routeId, limit));
    }
    
    @Test
    void testGetRateLimitInfo() {
        String clientKey = "test-client";
        Long routeId = 1L;
        int limit = 10;
        
        // Consume 3 tokens
        rateLimiter.tryAcquire(clientKey, routeId, limit);
        rateLimiter.tryAcquire(clientKey, routeId, limit);
        rateLimiter.tryAcquire(clientKey, routeId, limit);
        
        RateLimiter.RateLimitInfo info = rateLimiter.getRateLimitInfo(clientKey, routeId, limit);
        
        assertEquals(limit, info.limit());
        assertEquals(7, info.remaining());
        assertTrue(info.resetIn() > 0);
    }
}
