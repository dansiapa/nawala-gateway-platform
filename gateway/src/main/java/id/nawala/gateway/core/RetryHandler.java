package id.nawala.gateway.core;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Set;
import java.util.function.Predicate;

@Component
@Slf4j
public class RetryHandler {
    
    // Retryable status codes
    private static final Set<Integer> RETRYABLE_STATUS = Set.of(502, 503, 504);
    
    // Retryable exceptions
    private static final Predicate<Throwable> RETRYABLE_EXCEPTION = ex ->
        ex instanceof java.net.ConnectException ||
        ex instanceof java.net.SocketTimeoutException ||
        ex instanceof java.io.IOException ||
        (ex.getMessage() != null && ex.getMessage().contains("Connection refused"));
    
    public <T> Mono<T> withRetry(Mono<T> mono, RetryConfig config) {
        if (config == null || config.maxRetries() <= 0) {
            return mono;
        }
        
        return mono.retryWhen(
            Retry.backoff(config.maxRetries(), Duration.ofMillis(config.initialDelayMs()))
                .maxBackoff(Duration.ofMillis(config.maxDelayMs()))
                .jitter(0.5)
                .filter(RETRYABLE_EXCEPTION)
                .doBeforeRetry(signal -> 
                    log.warn("Retry attempt {} for: {}", 
                            signal.totalRetries() + 1, 
                            signal.failure().getMessage())
                )
                .onRetryExhaustedThrow((spec, signal) -> signal.failure())
        );
    }
    
    public boolean isRetryableStatus(int statusCode) {
        return RETRYABLE_STATUS.contains(statusCode);
    }
    
    public boolean isRetryableException(Throwable ex) {
        return RETRYABLE_EXCEPTION.test(ex);
    }
    
    public record RetryConfig(
        int maxRetries,
        long initialDelayMs,
        long maxDelayMs
    ) {
        public static RetryConfig defaultConfig() {
            return new RetryConfig(3, 100, 2000);
        }
        
        public static RetryConfig noRetry() {
            return new RetryConfig(0, 0, 0);
        }
    }
}
