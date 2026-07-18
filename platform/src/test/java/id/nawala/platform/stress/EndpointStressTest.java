package id.nawala.platform.stress;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Stress Tests - HTTP Endpoint Load")
class EndpointStressTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Login page handles 100 concurrent requests")
    void loginPageConcurrentLoad() throws Exception {
        int threadCount = 100;
        ExecutorService executor = Executors.newFixedThreadPool(20);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger success = new AtomicInteger(0);
        AtomicInteger failure = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    mockMvc.perform(get("/login"))
                            .andExpect(status().isOk());
                    success.incrementAndGet();
                } catch (Exception e) {
                    failure.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();
        assertThat(success.get()).isEqualTo(threadCount);
        assertThat(failure.get()).isZero();
    }

    @Test
    @DisplayName("Register page handles 50 concurrent requests")
    void registerPageConcurrentLoad() throws Exception {
        int threadCount = 50;
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger success = new AtomicInteger(0);
        AtomicInteger failure = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    mockMvc.perform(get("/register"))
                            .andExpect(status().isOk());
                    success.incrementAndGet();
                } catch (Exception e) {
                    failure.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();
        assertThat(success.get()).isEqualTo(threadCount);
        assertThat(failure.get()).isZero();
    }

    @Test
    @DisplayName("Internal API handles 200 concurrent requests")
    void internalApiConcurrentLoad() throws Exception {
        int threadCount = 200;
        ExecutorService executor = Executors.newFixedThreadPool(30);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger success = new AtomicInteger(0);
        AtomicInteger failure = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    mockMvc.perform(get("/internal/routes")
                                    .header("X-Internal-Secret", "TestInternalSecret123"))
                            .andExpect(status().isOk());
                    success.incrementAndGet();
                } catch (Exception e) {
                    failure.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();
        assertThat(success.get()).isEqualTo(threadCount);
        assertThat(failure.get()).isZero();
    }

    @Test
    @DisplayName("Rapid sequential requests don't cause errors")
    void rapidSequentialRequests() throws Exception {
        for (int i = 0; i < 50; i++) {
            mockMvc.perform(get("/login")).andExpect(status().isOk());
        }
    }
}
