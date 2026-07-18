package id.nawala.platform.service;

import id.nawala.platform.model.ApiKey;
import id.nawala.platform.model.User;
import id.nawala.platform.repository.ApiKeyRepository;
import id.nawala.platform.service.impl.ApiKeyServiceImpl;
import id.nawala.platform.service.WebhookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApiKeyService Regression Tests")
class ApiKeyServiceTest {

    @Mock
    private ApiKeyRepository apiKeyRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private WebhookService webhookService;
    @InjectMocks
    private ApiKeyServiceImpl apiKeyService;

    private User owner;

    @BeforeEach
    void setUp() {
        owner = User.builder().id(1L).username("admin").build();
    }

    @Nested
    @DisplayName("Generate Key")
    class GenerateTests {
        @Test
        void generateSuccess() {
            when(passwordEncoder.encode(anyString())).thenReturn("$2a$12$keyHash");
            when(apiKeyRepository.save(any(ApiKey.class))).thenAnswer(i -> {
                ApiKey k = i.getArgument(0); k.setId(1L); return k;
            });
            ApiKey result = apiKeyService.generate("test-key", owner, 30);
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("test-key");
            // keyHash is replaced with raw key after save
            assertThat(result.getKeyHash()).startsWith("nwl_");
        }

        @Test
        void generateWithScope() {
            when(passwordEncoder.encode(anyString())).thenReturn("$2a$12$hash");
            when(apiKeyRepository.save(any(ApiKey.class))).thenAnswer(i -> {
                ApiKey k = i.getArgument(0); k.setId(2L); return k;
            });
            ApiKey result = apiKeyService.generateWithScope("scoped", owner, 7,
                    1000, 30000, "192.168.1.1", "1,2,3", "GET,POST");
            assertThat(result.getName()).isEqualTo("scoped");
        }
    }

    @Nested
    @DisplayName("Revoke Key")
    class RevokeTests {
        @Test
        void revokeSuccess() {
            ApiKey key = ApiKey.builder().id(1L).name("k").active(true)
                    .prefix("nwl_test").build();
            when(apiKeyRepository.findById(1L)).thenReturn(Optional.of(key));
            when(apiKeyRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            apiKeyService.revoke(1L);
            assertThat(key.isActive()).isFalse();
        }
    }

    @Nested
    @DisplayName("Active Key Count")
    class CountTests {
        @Test
        void activeCount() {
            when(apiKeyRepository.countByActiveTrue()).thenReturn(5L);
            assertThat(apiKeyService.getActiveKeyCount()).isEqualTo(5L);
        }
    }

    @Nested
    @DisplayName("Find By Owner")
    class FindTests {
        @Test
        void findByOwner() {
            ApiKey k1 = ApiKey.builder().id(1L).name("k1").owner(owner).build();
            ApiKey k2 = ApiKey.builder().id(2L).name("k2").owner(owner).build();
            when(apiKeyRepository.findByOwner(owner)).thenReturn(List.of(k1, k2));
            List<ApiKey> result = apiKeyService.findByOwner(owner);
            assertThat(result).hasSize(2);
        }
    }
}
