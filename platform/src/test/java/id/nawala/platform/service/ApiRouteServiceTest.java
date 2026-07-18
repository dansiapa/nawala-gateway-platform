package id.nawala.platform.service;

import id.nawala.platform.exception.ResourceNotFoundException;
import id.nawala.platform.model.ApiRoute;
import id.nawala.platform.model.User;
import id.nawala.platform.repository.ApiRouteRepository;
import id.nawala.platform.service.impl.ApiRouteServiceImpl;
import id.nawala.platform.viewmodel.ApiRouteViewModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApiRouteService Regression Tests")
class ApiRouteServiceTest {

    @Mock
    private ApiRouteRepository apiRouteRepository;
    @InjectMocks
    private ApiRouteServiceImpl apiRouteService;

    private ApiRouteViewModel validForm;
    private User owner;
    private ApiRoute existingRoute;

    @BeforeEach
    void setUp() {
        validForm = new ApiRouteViewModel("Test Route", "Test desc", "GET",
                "/api/test", null, "http://localhost:9999", true, true, 100, false, null);
        owner = User.builder().id(1L).username("admin").build();
        existingRoute = ApiRoute.builder().id(1L).name("Existing")
                .method("GET").path("/api/existing")
                .targetUrl("http://localhost:9999")
                .active(true).rateLimitPerMinute(60).build();
    }

    @Nested
    @DisplayName("Register Route")
    class RegisterTests {
        @Test
        void registerSuccess() {
            when(apiRouteRepository.existsByPathAndMethod("/api/test", "GET")).thenReturn(false);
            when(apiRouteRepository.save(any())).thenAnswer(i -> {
                ApiRoute r = i.getArgument(0); r.setId(1L); return r;
            });
            ApiRoute result = apiRouteService.register(validForm, owner);
            assertThat(result.getName()).isEqualTo("Test Route");
            assertThat(result.getMethod()).isEqualTo("GET");
        }

        @Test
        void registerDuplicate() {
            when(apiRouteRepository.existsByPathAndMethod("/api/test", "GET")).thenReturn(true);
            assertThatThrownBy(() -> apiRouteService.register(validForm, owner))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Update Route")
    class UpdateTests {
        @Test
        void updateSuccess() {
            when(apiRouteRepository.findById(1L)).thenReturn(Optional.of(existingRoute));
            when(apiRouteRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            ApiRoute result = apiRouteService.update(1L, validForm);
            assertThat(result.getName()).isEqualTo("Test Route");
        }

        @Test
        void updateNotFound() {
            when(apiRouteRepository.findById(99L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> apiRouteService.update(99L, validForm))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Delete Route")
    class DeleteTests {
        @Test
        void deleteSuccess() {
            when(apiRouteRepository.existsById(1L)).thenReturn(true);
            apiRouteService.delete(1L);
            verify(apiRouteRepository).deleteById(1L);
        }

        @Test
        void deleteNotFound() {
            when(apiRouteRepository.existsById(99L)).thenReturn(false);
            assertThatThrownBy(() -> apiRouteService.delete(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Toggle Active")
    class ToggleTests {
        @Test
        void toggleActive() {
            when(apiRouteRepository.findById(1L)).thenReturn(Optional.of(existingRoute));
            when(apiRouteRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            apiRouteService.toggleActive(1L);
            assertThat(existingRoute.isActive()).isFalse();
        }
    }

    @Nested
    @DisplayName("Statistics")
    class StatsTests {
        @Test
        void totalRoutes() {
            when(apiRouteRepository.count()).thenReturn(10L);
            assertThat(apiRouteService.getTotalRoutes()).isEqualTo(10L);
        }
        @Test
        void activeRoutes() {
            when(apiRouteRepository.countByActive(true)).thenReturn(7L);
            assertThat(apiRouteService.getActiveRoutes()).isEqualTo(7L);
        }
    }
}
