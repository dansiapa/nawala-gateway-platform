package id.nawala.platform.controller;

import id.nawala.platform.model.User;
import id.nawala.platform.service.*;
import id.nawala.platform.viewmodel.DashboardViewModel;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final UserService userService;
    private final ActivityLogService activityLogService;
    private final ApiRouteService apiRouteService;
    private final ApiKeyService apiKeyService;
    private final HealthMonitorService healthMonitorService;
    private final AnomalyDetectionService anomalyDetectionService;

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.findByUsername(userDetails.getUsername())
                .orElseThrow();

        String initials = extractInitials(user.getFullName());

        HealthMonitorService.HealthSummary health = healthMonitorService.getHealthSummary();
        AnomalyDetectionService.ThreatStats threats = anomalyDetectionService.getThreatStats();

        DashboardViewModel vm = DashboardViewModel.builder()
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .initials(initials)
                .memberSince(user.getCreatedAt())
                .lastLogin(user.getLastLoginAt())
                .totalUsers(userService.getTotalUsers())
                .activeUsers(userService.getActiveUsers())
                .totalRoutes(apiRouteService.getTotalRoutes())
                .activeRoutes(apiRouteService.getActiveRoutes())
                .activeApiKeys(apiKeyService.getActiveKeyCount())
                .recentActivities(activityLogService.getRecentActivities(user))
                // Health
                .healthUp(health.upCount())
                .healthDown(health.downCount())
                .healthDegraded(health.degradedCount())
                .healthTotal(health.totalMonitored())
                // Threats
                .unresolvedThreats(threats.unresolvedCount())
                .criticalThreats(threats.criticalCount())
                .blockedSources(threats.blockedCount())
                .threats24h(threats.last24hCount())
                .build();

        model.addAttribute("vm", vm);
        return "dashboard";
    }

    private String extractInitials(String fullName) {
        if (fullName == null || fullName.isBlank()) return "?";
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, 1).toUpperCase();
        return (parts[0].charAt(0) + "" + parts[parts.length - 1].charAt(0)).toUpperCase();
    }
}

