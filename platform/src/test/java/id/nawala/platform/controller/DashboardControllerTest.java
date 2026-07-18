package id.nawala.platform.controller;

import id.nawala.platform.model.Role;
import id.nawala.platform.model.User;
import id.nawala.platform.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Dashboard Controller Regression Tests")
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        if (userRepository.findByUsername("dashtest").isEmpty()) {
            User user = User.builder()
                    .username("dashtest")
                    .email("dashtest@nawala.id")
                    .fullName("Dashboard Tester")
                    .password(passwordEncoder.encode("Test1234!"))
                    .role(Role.USER)
                    .enabled(true)
                    .build();
            userRepository.save(user);
        }
    }

    @Test
    @WithMockUser(username = "dashtest", roles = {"USER"})
    @DisplayName("GET /dashboard returns 200 for authenticated user")
    void dashboardReturns200() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("vm"));
    }

    @Test
    @WithMockUser(username = "dashtest", roles = {"USER"})
    @DisplayName("GET /profile returns 200")
    void profileReturns200() throws Exception {
        mockMvc.perform(get("/profile"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "dashtest", roles = {"USER"})
    @DisplayName("GET /routes returns 200")
    void routesReturns200() throws Exception {
        mockMvc.perform(get("/routes"))
                .andExpect(status().isOk())
                .andExpect(view().name("routes/list"));
    }

    @Test
    @WithMockUser(username = "dashtest", roles = {"USER"})
    @DisplayName("GET /api-keys returns 200")
    void apiKeysReturns200() throws Exception {
        mockMvc.perform(get("/api-keys"))
                .andExpect(status().isOk())
                .andExpect(view().name("apikeys/list"));
    }

    @Test
    @WithMockUser(username = "dashtest", roles = {"USER"})
    @DisplayName("GET /routes/new returns 200")
    void newRouteFormReturns200() throws Exception {
        mockMvc.perform(get("/routes/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("routes/form"));
    }
}
