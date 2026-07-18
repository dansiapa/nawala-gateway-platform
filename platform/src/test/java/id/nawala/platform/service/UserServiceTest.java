package id.nawala.platform.service;

import id.nawala.platform.exception.UserAlreadyExistsException;
import id.nawala.platform.model.Role;
import id.nawala.platform.model.User;
import id.nawala.platform.repository.UserRepository;
import id.nawala.platform.service.impl.UserServiceImpl;
import id.nawala.platform.viewmodel.ProfileViewModel;
import id.nawala.platform.viewmodel.RegisterViewModel;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Regression Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @InjectMocks
    private UserServiceImpl userService;

    private RegisterViewModel validForm;
    private User existingUser;

    @BeforeEach
    void setUp() {
        validForm = new RegisterViewModel("Test User", "testuser",
                "test@nawala.id", "Password123!", "Password123!");
        existingUser = User.builder().id(1L).username("testuser")
                .email("test@nawala.id").fullName("Test User")
                .password("$2a$12$hash").role(Role.USER)
                .enabled(true).createdAt(LocalDateTime.now()).build();
    }

    @Nested
    @DisplayName("Register")
    class RegisterTests {
        @Test
        void registerSuccess() {
            when(userRepository.existsByUsername("testuser")).thenReturn(false);
            when(userRepository.existsByEmail("test@nawala.id")).thenReturn(false);
            when(passwordEncoder.encode("Password123!")).thenReturn("$2a$12$hash");
            when(userRepository.save(any(User.class))).thenAnswer(i -> {
                User u = i.getArgument(0); u.setId(1L); return u;
            });
            User result = userService.register(validForm);
            assertThat(result.getUsername()).isEqualTo("testuser");
            assertThat(result.getRole()).isEqualTo(Role.USER);
            verify(userRepository).save(any(User.class));
        }

        @Test
        void duplicateUsername() {
            when(userRepository.existsByUsername("testuser")).thenReturn(true);
            assertThatThrownBy(() -> userService.register(validForm))
                    .isInstanceOf(UserAlreadyExistsException.class);
        }

        @Test
        void duplicateEmail() {
            when(userRepository.existsByUsername("testuser")).thenReturn(false);
            when(userRepository.existsByEmail("test@nawala.id")).thenReturn(true);
            assertThatThrownBy(() -> userService.register(validForm))
                    .isInstanceOf(UserAlreadyExistsException.class);
        }
    }

    @Nested
    @DisplayName("FindByUsername")
    class FindTests {
        @Test
        void findExisting() {
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(existingUser));
            assertThat(userService.findByUsername("testuser")).isPresent();
        }
        @Test
        void findMissing() {
            when(userRepository.findByUsername("x")).thenReturn(Optional.empty());
            assertThat(userService.findByUsername("x")).isEmpty();
        }
    }

    @Nested
    @DisplayName("Statistics")
    class StatsTests {
        @Test
        void totalUsers() {
            when(userRepository.count()).thenReturn(42L);
            assertThat(userService.getTotalUsers()).isEqualTo(42L);
        }
        @Test
        void activeUsers() {
            when(userRepository.countByEnabled(true)).thenReturn(35L);
            assertThat(userService.getActiveUsers()).isEqualTo(35L);
        }
    }
}
