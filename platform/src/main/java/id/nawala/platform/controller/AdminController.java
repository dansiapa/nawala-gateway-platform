package id.nawala.platform.controller;

import id.nawala.platform.model.Role;
import id.nawala.platform.model.User;
import id.nawala.platform.repository.*;
import id.nawala.platform.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Duration;
import java.time.LocalDateTime;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepository;
    private final ApiRouteRepository routeRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final AuditLogRepository auditLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    // ========== DASHBOARD ==========
    @GetMapping
    public String dashboard(Model model) {
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/dashboard")
    public String adminDashboard(Model model) {
        // Stats
        var stats = new AdminStats(
                userRepository.count(),
                routeRepository.count(),
                apiKeyRepository.count(),
                0L, // TODO: requestsToday from analytics
                formatUptime()
        );
        model.addAttribute("stats", stats);
        
        // Recent logs
        var recentLogs = auditLogRepository.findAll(
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"))
        ).getContent();
        model.addAttribute("recentLogs", recentLogs);
        
        return "admin/dashboard";
    }

    // ========== USER MANAGEMENT ==========
    @GetMapping("/users")
    public String users(Model model) {
        model.addAttribute("users", userRepository.findAll(Sort.by("id")));
        return "admin/users";
    }

    @PostMapping("/users/create")
    public String createUser(
            @RequestParam String username,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam Role role,
            @RequestParam(defaultValue = "false") boolean canManageRoutes,
            @RequestParam(defaultValue = "false") boolean canManageKeys,
            @RequestParam(defaultValue = "false") boolean canViewAnalytics,
            @AuthenticationPrincipal UserDetails adminDetails,
            RedirectAttributes redirectAttributes) {
        
        // Check if username or email exists
        if (userRepository.existsByUsername(username)) {
            redirectAttributes.addFlashAttribute("error", "Username already exists");
            return "redirect:/admin/users";
        }
        if (userRepository.existsByEmail(email)) {
            redirectAttributes.addFlashAttribute("error", "Email already exists");
            return "redirect:/admin/users";
        }
        
        User user = User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(password))
                .role(role)
                .enabled(true)
                .canManageRoutes(canManageRoutes)
                .canManageKeys(canManageKeys)
                .canViewAnalytics(canViewAnalytics)
                .build();
        
        userRepository.save(user);
        auditService.log("CREATE", "USER", "Created user: " + username, adminDetails.getUsername());
        
        redirectAttributes.addFlashAttribute("success", "User created successfully");
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/toggle")
    public String toggleUser(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails adminDetails,
            RedirectAttributes redirectAttributes) {
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Prevent disabling yourself
        if (user.getUsername().equals(adminDetails.getUsername())) {
            redirectAttributes.addFlashAttribute("error", "Cannot disable your own account");
            return "redirect:/admin/users";
        }
        
        user.setEnabled(!user.isEnabled());
        userRepository.save(user);
        
        String action = user.isEnabled() ? "enabled" : "disabled";
        auditService.log("UPDATE", "USER", "User " + action + ": " + user.getUsername(), adminDetails.getUsername());
        
        redirectAttributes.addFlashAttribute("success", "User " + action + " successfully");
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/update")
    public String updateUser(
            @PathVariable Long id,
            @RequestParam Role role,
            @RequestParam(defaultValue = "false") boolean canManageRoutes,
            @RequestParam(defaultValue = "false") boolean canManageKeys,
            @RequestParam(defaultValue = "false") boolean canViewAnalytics,
            @AuthenticationPrincipal UserDetails adminDetails,
            RedirectAttributes redirectAttributes) {
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setRole(role);
        user.setCanManageRoutes(canManageRoutes);
        user.setCanManageKeys(canManageKeys);
        user.setCanViewAnalytics(canViewAnalytics);
        userRepository.save(user);
        
        auditService.log("UPDATE", "USER", "Updated user: " + user.getUsername(), adminDetails.getUsername());
        redirectAttributes.addFlashAttribute("success", "User updated successfully");
        
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/reset-password")
    public String resetPassword(
            @PathVariable Long id,
            @RequestParam String newPassword,
            @AuthenticationPrincipal UserDetails adminDetails,
            RedirectAttributes redirectAttributes) {
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (newPassword.length() < 8) {
            redirectAttributes.addFlashAttribute("error", "Password must be at least 8 characters");
            return "redirect:/admin/users";
        }
        
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        
        auditService.log("UPDATE", "USER", "Password reset for: " + user.getUsername(), adminDetails.getUsername());
        redirectAttributes.addFlashAttribute("success", "Password reset successfully");
        
        return "redirect:/admin/users";
    }

    // ========== AUDIT LOGS ==========
    @GetMapping("/audit")
    public String auditLogs(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String username,
            @RequestParam(defaultValue = "0") int page,
            Model model) {
        
        var pageable = PageRequest.of(page, 50, Sort.by(Sort.Direction.DESC, "createdAt"));
        var logs = auditLogRepository.findAll(pageable);
        
        model.addAttribute("logs", logs.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", logs.getTotalPages());
        
        return "admin/audit";
    }

    // ========== SERVICE TIERS ==========
    @GetMapping("/tiers")
    public String tiers(Model model) {
        // TODO: Implement tier management
        model.addAttribute("tiers", java.util.List.of());
        return "admin/tiers";
    }

    // Helper methods
    private String formatUptime() {
        // Simple uptime calculation (in real app, track actual start time)
        return "Running";
    }

    record AdminStats(long totalUsers, long totalRoutes, long totalApiKeys, long requestsToday, String uptime) {}
}
