package id.nawala.platform.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Security Config Regression Tests")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Public pages accessible without auth")
    void publicPages() throws Exception {
        mockMvc.perform(get("/login")).andExpect(status().isOk());
        mockMvc.perform(get("/register")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Static resources accessible without auth")
    void staticResources() throws Exception {
        // Static resource paths are permitted without authentication (no redirect to login)
        mockMvc.perform(get("/css/nonexist.css"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    // Should NOT redirect to login (302 to /login) - path is publicly accessible
                    assertThat(status).isNotEqualTo(302);
                });
        mockMvc.perform(get("/js/nonexist.js"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assertThat(status).isNotEqualTo(302);
                });
    }

    @Test
    @DisplayName("Admin page requires ADMIN role")
    void adminRequiresRole() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    @DisplayName("Normal user cannot access /admin")
    void normalUserDeniedAdmin() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Internal API without secret returns 403")
    void internalApiNoSecret() throws Exception {
        mockMvc.perform(get("/internal/routes"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Internal API with correct secret returns 200")
    void internalApiWithSecret() throws Exception {
        mockMvc.perform(get("/internal/routes")
                        .header("X-Internal-Secret", "TestInternalSecret123"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Internal API with wrong secret returns 403")
    void internalApiWrongSecret() throws Exception {
        mockMvc.perform(get("/internal/routes")
                        .header("X-Internal-Secret", "WrongSecret"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Logout redirects to login page")
    @WithMockUser(username = "user", roles = {"USER"})
    void logoutRedirects() throws Exception {
        mockMvc.perform(post("/logout").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout=true"));
    }
}
