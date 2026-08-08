package id.nawala.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Slf4j
public class RateLimiter {
    
    // Key: routeId:clientKey, Value: TokenBucket
    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();
    
    public boolean tryAcquire(String clientKey, Long routeId, int limitPerMinute) {
        if (limitPerMinute <= 0) {
            return true; // No limit
        }
        
        String key = routeId + ":" + clientKey;
        TokenBucket bucket = buckets.computeIfAbsent(key, k -> new TokenBucket(limitPerMinute));
        
        boolean allowed = bucket.tryConsume();
        if (!allowed) {
            log.warn("Rate limit exceeded for {} on route {}", clientKey, routeId);
        }
        return allowed;
    }
    
    public RateLimitInfo getRateLimitInfo(String clientKey, Long routeId, int limitPerMinute) {
        if (limitPerMinute <= 0) {
            return new RateLimitInfo(Integer.MAX_VALUE, Integer.MAX_VALUE, 0);
        }
        
        String key = routeId + ":" + clientKey;
        TokenBucket bucket = buckets.get(key);
        
        if (bucket == null) {
            return new RateLimitInfo(limitPerMinute, limitPerMinute, 0);
        }
        
        return new RateLimitInfo(limitPerMinute, bucket.getAvailableTokens(), bucket.getResetTimeSeconds());
    }
    
    public record RateLimitInfo(int limit, int remaining, long resetIn) {}
    
    private static class TokenBucket {
        private final int maxTokens;
        private final AtomicInteger tokens;
        private volatile long lastRefillTime;
        private final long refillIntervalMs = 60_000; // 1 minute
        
        TokenBucket(int maxTokens) {
            this.maxTokens = maxTokens;
            this.tokens = new AtomicInteger(maxTokens);
            this.lastRefillTime = System.currentTimeMillis();
        }
        
        synchronized boolean tryConsume() {
            refillIfNeeded();
            if (tokens.get() > 0) {
                tokens.decrementAndGet();
                return true;
            }
            return false;
        }
        
        int getAvailableTokens() {
            refillIfNeeded();
            return tokens.get();
        }
        
        long getResetTimeSeconds() {
            long elapsed = System.currentTimeMillis() - lastRefillTime;
            return Math.max(0, (refillIntervalMs - elapsed) / 1000);
        }
        
        private void refillIfNeeded() {
            long now = System.currentTimeMillis();
            if (now - lastRefillTime >= refillIntervalMs) {
                tokens.set(maxTokens);
                lastRefillTime = now;
            }
        }
    }
    
    // Cleanup old buckets periodically
    public void cleanup() {
        long now = System.currentTimeMillis();
        buckets.entrySet().removeIf(entry -> 
            now - entry.getValue().lastRefillTime > 300_000 // 5 minutes idle
        );
    }
}
