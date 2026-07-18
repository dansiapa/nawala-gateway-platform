package id.nawala.platform.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Auth Controller Regression Tests")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /login returns 200")
    void loginPageReturns200() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    @Test
    @DisplayName("GET /register returns 200")
    void registerPageReturns200() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));
    }

    @Test
    @DisplayName("POST /login with invalid credentials")
    void loginInvalidCredentials() throws Exception {
        mockMvc.perform(post("/login").with(csrf())
                        .param("username", "invalid")
                        .param("password", "wrong"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error=true"));
    }

    @Test
    @DisplayName("POST /register with valid data")
    void registerSuccess() throws Exception {
        mockMvc.perform(post("/register").with(csrf())
                        .param("fullName", "Integration Test")
                        .param("username", "integrationuser")
                        .param("email", "integration@nawala.id")
                        .param("password", "TestPass123!")
                        .param("confirmPassword", "TestPass123!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("POST /register with mismatched passwords")
    void registerPasswordMismatch() throws Exception {
        mockMvc.perform(post("/register").with(csrf())
                        .param("fullName", "Test")
                        .param("username", "mismatchuser")
                        .param("email", "mm@nawala.id")
                        .param("password", "TestPass123!")
                        .param("confirmPassword", "Different123!"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));
    }

    @Test
    @DisplayName("POST /register with blank fields returns errors")
    void registerBlankFields() throws Exception {
        mockMvc.perform(post("/register").with(csrf())
                        .param("fullName", "")
                        .param("username", "")
                        .param("email", "")
                        .param("password", "")
                        .param("confirmPassword", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));
    }

    @Test
    @DisplayName("Unauthenticated access to /dashboard redirects to login")
    void dashboardRequiresAuth() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("Unauthenticated access to /routes redirects to login")
    void routesRequiresAuth() throws Exception {
        mockMvc.perform(get("/routes"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("Unauthenticated access to /api-keys redirects to login")
    void apiKeysRequiresAuth() throws Exception {
        mockMvc.perform(get("/api-keys"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("Unauthenticated access to /admin/users redirects to login")
    void adminRequiresAuth() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().is3xxRedirection());
    }
}
