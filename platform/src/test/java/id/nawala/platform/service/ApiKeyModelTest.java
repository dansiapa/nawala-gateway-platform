package id.nawala.platform.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@DisplayName("ApiKey Model Regression Tests")
class ApiKeyModelTest {

    @Nested
    @DisplayName("Expiration")
    class ExpirationTests {
        @Test
        void notExpired() {
            ApiKey key = ApiKey.builder().expiresAt(LocalDateTime.now().plusDays(1)).build();
            assertThat(key.isExpired()).isFalse();
        }
        @Test
        void expired() {
            ApiKey key = ApiKey.builder().expiresAt(LocalDateTime.now().minusDays(1)).build();
            assertThat(key.isExpired()).isTrue();
        }
        @Test
        void nullExpiration() {
            ApiKey key = ApiKey.builder().expiresAt(null).build();
            assertThat(key.isExpired()).isFalse();
        }
    }

    @Nested
    @DisplayName("IP Restriction")
    class IpTests {
        @Test
        void allowedIpMatch() {
            ApiKey key = ApiKey.builder().allowedIps("192.168.1.1,10.0.0.1").build();
            assertThat(key.isIpAllowed("192.168.1.1")).isTrue();
            assertThat(key.isIpAllowed("10.0.0.1")).isTrue();
        }
        @Test
        void ipDenied() {
            ApiKey key = ApiKey.builder().allowedIps("192.168.1.1").build();
            assertThat(key.isIpAllowed("10.0.0.99")).isFalse();
        }
        @Test
        void emptyIpsAllowAll() {
            ApiKey key = ApiKey.builder().allowedIps(null).build();
            assertThat(key.isIpAllowed("anything")).isTrue();
        }
    }

    @Nested
    @DisplayName("Method Restriction")
    class MethodTests {
        @Test
        void allowedMethod() {
            ApiKey key = ApiKey.builder().allowedMethods("GET,POST").build();
            assertThat(key.isMethodAllowed("GET")).isTrue();
            assertThat(key.isMethodAllowed("post")).isTrue();
        }
        @Test
        void deniedMethod() {
            ApiKey key = ApiKey.builder().allowedMethods("GET").build();
            assertThat(key.isMethodAllowed("DELETE")).isFalse();
        }
        @Test
        void emptyAllowsAll() {
            ApiKey key = ApiKey.builder().allowedMethods(null).build();
            assertThat(key.isMethodAllowed("PUT")).isTrue();
        }
    }

    @Nested
    @DisplayName("Route Restriction")
    class RouteTests {
        @Test
        void allowedRoute() {
            ApiKey key = ApiKey.builder().allowedRoutes("1,2,3").build();
            assertThat(key.isRouteAllowed(2L)).isTrue();
        }
        @Test
        void deniedRoute() {
            ApiKey key = ApiKey.builder().allowedRoutes("1,2").build();
            assertThat(key.isRouteAllowed(5L)).isFalse();
        }
        @Test
        void emptyAllowsAll() {
            ApiKey key = ApiKey.builder().allowedRoutes(null).build();
            assertThat(key.isRouteAllowed(99L)).isTrue();
        }
    }

    @Nested
    @DisplayName("Quota")
    class QuotaTests {
        @Test
        void dailyQuotaExceeded() {
            ApiKey key = ApiKey.builder().dailyQuota(100).dailyUsage(100).build();
            assertThat(key.isQuotaExceeded()).isTrue();
        }
        @Test
        void monthlyQuotaExceeded() {
            ApiKey key = ApiKey.builder().monthlyQuota(1000).monthlyUsage(1000).build();
            assertThat(key.isQuotaExceeded()).isTrue();
        }
        @Test
        void quotaNotExceeded() {
            ApiKey key = ApiKey.builder().dailyQuota(100).dailyUsage(50)
                    .monthlyQuota(1000).monthlyUsage(500).build();
            assertThat(key.isQuotaExceeded()).isFalse();
        }
        @Test
        void unlimitedQuota() {
            ApiKey key = ApiKey.builder().dailyQuota(0).monthlyQuota(0).build();
            assertThat(key.isQuotaExceeded()).isFalse();
        }
    }
}
