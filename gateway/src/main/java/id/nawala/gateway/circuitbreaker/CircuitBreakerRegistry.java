package id.nawala.gateway.circuitbreaker;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Circuit Breaker implementation per route target.
 * States: CLOSED (normal) -> OPEN (failing) -> HALF_OPEN (testing)
 */
@Component
@Slf4j
public class CircuitBreakerRegistry {

    private final Map<String, CircuitState> circuits = new ConcurrentHashMap<>();

    private static final int FAILURE_THRESHOLD = 3;
    private static final long OPEN_DURATION_MS = 30_000; // 30 sec before half-open
    private static final long HALF_OPEN_TIMEOUT_MS = 10_000;

    public enum State { CLOSED, OPEN, HALF_OPEN }

    public State getState(String targetKey) {
        CircuitState cs = circuits.get(targetKey);
        if (cs == null) return State.CLOSED;

        if (cs.state == State.OPEN) {
            if (Instant.now().toEpochMilli() - cs.openedAt > OPEN_DURATION_MS) {
                cs.state = State.HALF_OPEN;
                cs.halfOpenAt = Instant.now().toEpochMilli();
                log.info("Circuit HALF_OPEN for {}", targetKey);
                return State.HALF_OPEN;
            }
        }
        return cs.state;
    }

    public boolean isCallPermitted(String targetKey) {
        State state = getState(targetKey);
        return state != State.OPEN;
    }

    public void recordSuccess(String targetKey) {
        CircuitState cs = circuits.get(targetKey);
        if (cs != null) {
            cs.failures.set(0);
            if (cs.state == State.HALF_OPEN) {
                cs.state = State.CLOSED;
                log.info("Circuit CLOSED for {} (recovered)", targetKey);
            }
        }
    }

    public void recordFailure(String targetKey) {
        CircuitState cs = circuits.computeIfAbsent(targetKey, k -> new CircuitState());
        int count = cs.failures.incrementAndGet();
        if (count >= FAILURE_THRESHOLD && cs.state == State.CLOSED) {
            cs.state = State.OPEN;
            cs.openedAt = Instant.now().toEpochMilli();
            log.warn("Circuit OPEN for {} after {} failures", targetKey, count);
        }
    }

    public Map<String, String> getAllStates() {
        Map<String, String> result = new ConcurrentHashMap<>();
        circuits.forEach((key, cs) -> result.put(key, getState(key).name()));
        return result;
    }

    private static class CircuitState {
        volatile State state = State.CLOSED;
        final AtomicInteger failures = new AtomicInteger(0);
        volatile long openedAt;
        volatile long halfOpenAt;
    }
}
