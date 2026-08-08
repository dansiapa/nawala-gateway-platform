package id.nawala.platform.controller;

import id.nawala.platform.model.User;
import id.nawala.platform.repository.ApiKeyRepository;
import id.nawala.platform.repository.ApiRouteRepository;
import id.nawala.platform.repository.UserRepository;
import id.nawala.platform.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UserRepository userRepository;
    private final ApiRouteRepository routeRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @GetMapping
    public String profile(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Add stats
        long routeCount = routeRepository.countByCreatedBy(user);
        long apiKeyCount = apiKeyRepository.countByOwner(user);
        
        model.addAttribute("user", user);
        model.addAttribute("routeCount", routeCount);
        model.addAttribute("apiKeyCount", apiKeyCount);
        
        return "profile";
    }

    @PostMapping("/update")
    public String updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String email,
            @RequestParam(required = false) String fullName,
            RedirectAttributes redirectAttributes) {
        
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setEmail(email);
        if (fullName != null) user.setFullName(fullName);
        userRepository.save(user);
        
        auditService.log("UPDATE", "USER", "Profile updated", user.getUsername());
        redirectAttributes.addFlashAttribute("success", "Profile updated successfully");
        
        return "redirect:/profile";
    }

    @PostMapping("/password")
    public String changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String currentPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            RedirectAttributes redirectAttributes) {
        
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Verify current password
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            redirectAttributes.addFlashAttribute("error", "Current password is incorrect");
            return "redirect:/profile";
        }
        
        // Verify new passwords match
        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "New passwords do not match");
            return "redirect:/profile";
        }
        
        // Validate new password
        if (newPassword.length() < 8) {
            redirectAttributes.addFlashAttribute("error", "Password must be at least 8 characters");
            return "redirect:/profile";
        }
        
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        
        auditService.log("UPDATE", "USER", "Password changed", user.getUsername());
        redirectAttributes.addFlashAttribute("success", "Password changed successfully");
        
        return "redirect:/profile";
    }

    @PostMapping("/theme")
    @ResponseBody
    public String updateTheme(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String theme) {
        
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (theme.matches("^(light|dark|system)$")) {
            user.setThemePreference(theme);
            userRepository.save(user);
            return "OK";
        }
        
        return "Invalid theme";
    }
}
