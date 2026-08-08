package id.nawala.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class ResponseCache {
    
    private final Map<String, CachedResponse> cache = new ConcurrentHashMap<>();
    private static final long DEFAULT_TTL_MS = 60_000; // 1 minute
    private static final int MAX_CACHE_SIZE = 10_000;
    
    public CachedResponse get(String cacheKey) {
        CachedResponse cached = cache.get(cacheKey);
        if (cached != null) {
            if (cached.isExpired()) {
                cache.remove(cacheKey);
                return null;
            }
            log.debug("Cache hit: {}", cacheKey);
            return cached;
        }
        return null;
    }
    
    public void put(String cacheKey, HttpStatus status, HttpHeaders headers, byte[] body, long ttlMs) {
        if (cache.size() >= MAX_CACHE_SIZE) {
            evictOldest();
        }
        
        cache.put(cacheKey, new CachedResponse(
            status, headers, body,
            System.currentTimeMillis(),
            ttlMs > 0 ? ttlMs : DEFAULT_TTL_MS
        ));
        log.debug("Cached: {} (TTL: {}ms)", cacheKey, ttlMs);
    }
    
    public void invalidate(String cacheKey) {
        cache.remove(cacheKey);
    }
    
    public void invalidatePattern(String pattern) {
        cache.keySet().removeIf(key -> key.matches(pattern));
    }
    
    public void clear() {
        cache.clear();
    }
    
    public int size() {
        return cache.size();
    }
    
    private void evictOldest() {
        // Remove expired entries first
        cache.entrySet().removeIf(e -> e.getValue().isExpired());
        
        // If still too large, remove oldest
        if (cache.size() >= MAX_CACHE_SIZE) {
            cache.entrySet().stream()
                .min((a, b) -> Long.compare(a.getValue().createdAt(), b.getValue().createdAt()))
                .ifPresent(e -> cache.remove(e.getKey()));
        }
    }
    
    public String buildCacheKey(String method, String path, String queryString, String varyHeaders) {
        StringBuilder key = new StringBuilder();
        key.append(method).append(":").append(path);
        if (queryString != null && !queryString.isBlank()) {
            key.append("?").append(queryString);
        }
        if (varyHeaders != null && !varyHeaders.isBlank()) {
            key.append("#").append(varyHeaders);
        }
        return key.toString();
    }
    
    public boolean isCacheable(String method, int statusCode) {
        // Only cache successful GET requests
        return "GET".equalsIgnoreCase(method) && statusCode >= 200 && statusCode < 300;
    }
    
    public record CachedResponse(
        HttpStatus status,
        HttpHeaders headers,
        byte[] body,
        long createdAt,
        long ttlMs
    ) {
        public boolean isExpired() {
            return System.currentTimeMillis() > createdAt + ttlMs;
        }
        
        public long ageMs() {
            return System.currentTimeMillis() - createdAt;
        }
    }
}
