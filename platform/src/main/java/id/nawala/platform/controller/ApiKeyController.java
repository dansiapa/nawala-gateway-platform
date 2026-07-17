package id.nawala.platform.controller;

import id.nawala.platform.model.ApiKey;
import id.nawala.platform.model.User;
import id.nawala.platform.service.ActivityLogService;
import id.nawala.platform.service.ApiKeyService;
import id.nawala.platform.service.AuditService;
import id.nawala.platform.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/api-keys")
@RequiredArgsConstructor
public class ApiKeyController {

    private final ApiKeyService apiKeyService;
    private final UserService userService;
    private final ActivityLogService activityLogService;
    private final AuditService auditService;

    @GetMapping
    public String listKeys(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.findByUsername(userDetails.getUsername()).orElseThrow();
        model.addAttribute("keys", apiKeyService.findByOwner(user));
        return "apikeys/list";
    }

    @PostMapping("/generate")
    public String generateKey(@AuthenticationPrincipal UserDetails userDetails,
                              @RequestParam String name,
                              @RequestParam(required = false) Integer expirationDays,
                              @RequestParam(defaultValue = "0") long dailyQuota,
                              @RequestParam(defaultValue = "0") long monthlyQuota,
                              @RequestParam(required = false) String allowedIps,
                              @RequestParam(required = false) String allowedRoutes,
                              @RequestParam(required = false) String allowedMethods,
                              HttpServletRequest request,
                              RedirectAttributes redirectAttributes) {
        User user = userService.findByUsername(userDetails.getUsername()).orElseThrow();
        ApiKey generated = apiKeyService.generateWithScope(name, user, expirationDays,
                dailyQuota, monthlyQuota, allowedIps, allowedRoutes, allowedMethods);
        activityLogService.log(user, "KEY_GENERATE", "Generated API key: " + name, null);
        auditService.log(user.getId(), user.getUsername(), "CREATE", "API_KEY",
                generated.getId(), "Key generated: " + name, request.getRemoteAddr());
        redirectAttributes.addFlashAttribute("success", "API Key generated successfully");
        redirectAttributes.addFlashAttribute("rawKey", generated.getKeyHash());
        return "redirect:/api-keys";
    }

    @PostMapping("/{id}/rotate")
    public String rotateKey(@PathVariable Long id,
                            @AuthenticationPrincipal UserDetails userDetails,
                            HttpServletRequest request,
                            RedirectAttributes redirectAttributes) {
        User user = userService.findByUsername(userDetails.getUsername()).orElseThrow();
        ApiKey rotated = apiKeyService.rotate(id);
        activityLogService.log(user, "KEY_ROTATE", "Rotated API key: #" + id, null);
        auditService.log(user.getId(), user.getUsername(), "ROTATE", "API_KEY",
                id, "Key rotated, grace period 24h", request.getRemoteAddr());
        redirectAttributes.addFlashAttribute("success", "API Key rotated. Old key valid for 24h grace period.");
        redirectAttributes.addFlashAttribute("rawKey", rotated.getKeyHash());
        return "redirect:/api-keys";
    }

    @PostMapping("/{id}/revoke")
    public String revokeKey(@PathVariable Long id,
                            @AuthenticationPrincipal UserDetails userDetails,
                            HttpServletRequest request,
                            RedirectAttributes redirectAttributes) {
        apiKeyService.revoke(id);
        User user = userService.findByUsername(userDetails.getUsername()).orElseThrow();
        activityLogService.log(user, "KEY_REVOKE", "Revoked API key: #" + id, null);
        auditService.log(user.getId(), user.getUsername(), "REVOKE", "API_KEY",
                id, "Key revoked", request.getRemoteAddr());
        redirectAttributes.addFlashAttribute("success", "API Key revoked successfully");
        return "redirect:/api-keys";
    }
}

