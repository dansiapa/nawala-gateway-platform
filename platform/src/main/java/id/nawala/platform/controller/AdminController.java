package id.nawala.platform.controller;

import id.nawala.platform.model.AuditLog;
import id.nawala.platform.model.RateLimitTier;
import id.nawala.platform.model.User;
import id.nawala.platform.repository.RateLimitTierRepository;
import id.nawala.platform.repository.UserRepository;
import id.nawala.platform.service.AuditService;
import id.nawala.platform.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final AuditService auditService;
    private final RateLimitTierRepository rateLimitTierRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    @GetMapping
    public String adminDashboard(Model model) {
        model.addAttribute("totalUsers", userRepository.count());
        model.addAttribute("activeUsers", userRepository.countByEnabled(true));
        model.addAttribute("tiers", rateLimitTierRepository.findAll());
        model.addAttribute("recentAudit", auditService.getRecent(50));
        return "admin/dashboard";
    }

    @GetMapping("/audit")
    public String auditLog(Model model) {
        List<AuditLog> logs = auditService.getRecent(200);
        model.addAttribute("logs", logs);
        return "admin/audit";
    }

    @GetMapping("/tiers")
    public String rateLimitTiers(Model model) {
        model.addAttribute("tiers", rateLimitTierRepository.findAll());
        return "admin/tiers";
    }

    @PostMapping("/tiers")
    public String createTier(@RequestParam String name,
                             @RequestParam int requestsPerMinute,
                             @RequestParam int requestsPerHour,
                             @RequestParam int requestsPerDay,
                             @RequestParam(defaultValue = "10") int burstSize,
                             @RequestParam(required = false) String description,
                             RedirectAttributes ra) {
        RateLimitTier tier = RateLimitTier.builder()
                .name(name.toUpperCase())
                .requestsPerMinute(requestsPerMinute)
                .requestsPerHour(requestsPerHour)
                .requestsPerDay(requestsPerDay)
                .burstSize(burstSize)
                .description(description)
                .active(true)
                .build();
        rateLimitTierRepository.save(tier);
        ra.addFlashAttribute("success", "Tier created: " + name);
        return "redirect:/admin/tiers";
    }

    @PostMapping("/tiers/{id}/delete")
    public String deleteTier(@PathVariable Long id, RedirectAttributes ra) {
        rateLimitTierRepository.deleteById(id);
        ra.addFlashAttribute("success", "Tier deleted");
        return "redirect:/admin/tiers";
    }

    @GetMapping("/users")
    public String manageUsers(Model model) {
        model.addAttribute("users", userRepository.findAll());
        return "admin/users";
    }

    @PostMapping("/users/{id}/toggle")
    public String toggleUser(@PathVariable Long id,
                             @AuthenticationPrincipal UserDetails currentUser,
                             RedirectAttributes ra) {
        userRepository.findById(id).ifPresent(u -> {
            // Prevent admin from disabling their own account
            if (u.getUsername().equals(currentUser.getUsername())) {
                ra.addFlashAttribute("error", "Cannot disable your own account");
                return;
            }
            u.setEnabled(!u.isEnabled());
            userRepository.save(u);
            ra.addFlashAttribute("success", "User updated");
        });
        return "redirect:/admin/users";
    }
}
