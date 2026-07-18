package id.nawala.platform.stress;

import id.nawala.platform.model.ApiKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Stress Tests - Concurrent Operations")
class ConcurrencyStressTest {

    @Test
    @DisplayName("ApiKey model handles concurrent quota checks safely")
    void concurrentQuotaCheck() throws Exception {
        ApiKey key = ApiKey.builder()
                .dailyQuota(1000).dailyUsage(999)
                .monthlyQuota(10000).monthlyUsage(5000)
                .build();

        int threadCount = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger exceeded = new AtomicInteger(0);
        AtomicInteger notExceeded = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    if (key.isQuotaExceeded()) {
                        exceeded.incrementAndGet();
                    } else {
                        notExceeded.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();
        // All threads should get consistent result
        assertThat(exceeded.get() + notExceeded.get()).isEqualTo(threadCount);
    }

    @Test
    @DisplayName("ApiKey IP check under concurrent load")
    void concurrentIpCheck() throws Exception {
        ApiKey key = ApiKey.builder()
                .allowedIps("192.168.1.1,10.0.0.1,172.16.0.1")
                .build();

        int threadCount = 200;
        ExecutorService executor = Executors.newFixedThreadPool(50);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger errors = new AtomicInteger(0);
        String[] testIps = {"192.168.1.1", "10.0.0.1", "172.16.0.1", "8.8.8.8"};

        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            executor.submit(() -> {
                try {
                    String ip = testIps[idx % testIps.length];
                    boolean allowed = key.isIpAllowed(ip);
                    boolean expected = !ip.equals("8.8.8.8");
                    if (allowed != expected) errors.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();
        assertThat(errors.get()).isZero();
    }

    @Test
    @DisplayName("Expiration check under concurrent load")
    void concurrentExpirationCheck() throws Exception {
        ApiKey validKey = ApiKey.builder()
                .expiresAt(LocalDateTime.now().plusHours(1)).build();
        ApiKey expiredKey = ApiKey.builder()
                .expiresAt(LocalDateTime.now().minusHours(1)).build();

        int threadCount = 200;
        ExecutorService executor = Executors.newFixedThreadPool(50);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger errors = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final boolean useValid = i % 2 == 0;
            executor.submit(() -> {
                try {
                    ApiKey k = useValid ? validKey : expiredKey;
                    boolean result = k.isExpired();
                    if (useValid && result) errors.incrementAndGet();
                    if (!useValid && !result) errors.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();
        assertThat(errors.get()).isZero();
    }
}
