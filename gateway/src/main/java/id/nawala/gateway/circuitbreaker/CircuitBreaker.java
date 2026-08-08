package id.nawala.gateway.circuitbreaker;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Slf4j
public class CircuitBreaker {
    
    private final Map<String, CircuitState> circuits = new ConcurrentHashMap<>();
    
    // Default settings
    private static final int FAILURE_THRESHOLD = 5;
    private static final long OPEN_DURATION_MS = 30_000; // 30 seconds
    private static final int HALF_OPEN_MAX_CALLS = 3;
    
    public boolean allowRequest(String target) {
        CircuitState state = circuits.computeIfAbsent(target, k -> new CircuitState());
        return state.allowRequest();
    }
    
    public void recordSuccess(String target) {
        CircuitState state = circuits.get(target);
        if (state != null) {
            state.recordSuccess();
        }
    }
    
    public void recordFailure(String target) {
        CircuitState state = circuits.computeIfAbsent(target, k -> new CircuitState());
        state.recordFailure();
    }
    
    public Status getStatus(String target) {
        CircuitState state = circuits.get(target);
        return state != null ? state.getStatus() : Status.CLOSED;
    }
    
    public void reset(String target) {
        circuits.remove(target);
        log.info("Circuit breaker reset for {}", target);
    }
    
    public enum Status {
        CLOSED,      // Normal operation
        OPEN,        // Failing, reject requests
        HALF_OPEN    // Testing if service recovered
    }
    
    private static class CircuitState {
        private volatile Status status = Status.CLOSED;
        private final AtomicInteger failureCount = new AtomicInteger(0);
        private final AtomicInteger successCount = new AtomicInteger(0);
        private final AtomicLong lastFailureTime = new AtomicLong(0);
        private final AtomicInteger halfOpenCalls = new AtomicInteger(0);
        
        synchronized boolean allowRequest() {
            switch (status) {
                case CLOSED:
                    return true;
                case OPEN:
                    if (System.currentTimeMillis() - lastFailureTime.get() > OPEN_DURATION_MS) {
                        status = Status.HALF_OPEN;
                        halfOpenCalls.set(0);
                        log.info("Circuit breaker transitioning to HALF_OPEN");
                        return true;
                    }
                    return false;
                case HALF_OPEN:
                    return halfOpenCalls.incrementAndGet() <= HALF_OPEN_MAX_CALLS;
                default:
                    return true;
            }
        }
        
        synchronized void recordSuccess() {
            if (status == Status.HALF_OPEN) {
                successCount.incrementAndGet();
                if (successCount.get() >= HALF_OPEN_MAX_CALLS) {
                    status = Status.CLOSED;
                    failureCount.set(0);
                    successCount.set(0);
                    log.info("Circuit breaker CLOSED - service recovered");
                }
            } else if (status == Status.CLOSED) {
                failureCount.set(0); // Reset on success
            }
        }
        
        synchronized void recordFailure() {
            lastFailureTime.set(System.currentTimeMillis());
            
            if (status == Status.HALF_OPEN) {
                status = Status.OPEN;
                log.warn("Circuit breaker OPEN - failure during half-open");
            } else if (status == Status.CLOSED) {
                if (failureCount.incrementAndGet() >= FAILURE_THRESHOLD) {
                    status = Status.OPEN;
                    log.warn("Circuit breaker OPEN - threshold reached");
                }
            }
        }
        
        Status getStatus() {
            return status;
        }
    }
}
