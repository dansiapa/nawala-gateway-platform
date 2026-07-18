package id.nawala.gateway.filter;

import id.nawala.gateway.filter.RateLimitFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpResponse;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("RateLimitFilter Regression Tests")
class RateLimitFilterTest {

    private RateLimitFilter rateLimitFilter;
    private GatewayFilterChain chain;

    @BeforeEach
    void setUp() {
        rateLimitFilter = new RateLimitFilter();
        chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());
    }

    @Test
    @DisplayName("Allows requests within limit")
    void allowsWithinLimit() {
        RateLimitFilter.Config config = new RateLimitFilter.Config();
        config.setLimit(10);
        GatewayFilter filter = rateLimitFilter.apply(config);

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/test")
                .header("X-API-Key", "nwl_testkey1234567890")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isNotEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        verify(chain).filter(any());
    }

    @Test
    @DisplayName("Blocks requests exceeding limit")
    void blocksExceedingLimit() {
        RateLimitFilter.Config config = new RateLimitFilter.Config();
        config.setLimit(5);
        GatewayFilter filter = rateLimitFilter.apply(config);

        for (int i = 0; i < 6; i++) {
            MockServerHttpRequest request = MockServerHttpRequest.get("/api/test")
                    .header("X-API-Key", "nwl_samekey12345678")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            filter.filter(exchange, chain).block();

            if (i < 5) {
                verify(chain, times(i + 1)).filter(any());
            } else {
                assertThat(exchange.getResponse().getStatusCode())
                        .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
            }
        }
    }

    @Test
    @DisplayName("Different clients have separate rate limits")
    void separateClientsRateLimits() {
        RateLimitFilter.Config config = new RateLimitFilter.Config();
        config.setLimit(2);
        GatewayFilter filter = rateLimitFilter.apply(config);

        // Client 1 - use 2 requests (key prefix resolves to "key:nwl_aaaa")
        for (int i = 0; i < 2; i++) {
            MockServerHttpRequest req = MockServerHttpRequest.get("/api/test")
                    .header("X-API-Key", "nwl_aaaa_client1_key")
                    .build();
            MockServerWebExchange ex = MockServerWebExchange.from(req);
            filter.filter(ex, chain).block();
        }

        // Client 2 - different first 8 chars, should still be allowed
        MockServerHttpRequest req2 = MockServerHttpRequest.get("/api/test")
                .header("X-API-Key", "nwl_bbbb_client2_key")
                .build();
        MockServerWebExchange ex2 = MockServerWebExchange.from(req2);
        filter.filter(ex2, chain).block();

        assertThat(ex2.getResponse().getStatusCode())
                .isNotEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }
}
