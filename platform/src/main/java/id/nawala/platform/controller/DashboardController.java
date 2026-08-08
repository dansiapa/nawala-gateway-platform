package id.nawala.platform.controller;

import id.nawala.platform.model.User;
import id.nawala.platform.repository.*;
import id.nawala.platform.service.ActivityLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final UserRepository userRepository;
    private final ApiRouteRepository routeRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final ActivityLogService activityLogService;

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Dashboard stats
        long activeRoutes = routeRepository.countByActive(true);
        long activeKeys = apiKeyRepository.countByActiveTrue();
        long healthyServices = routeRepository.countByHealthStatus("UP");
        long downServices = routeRepository.countByHealthStatus("DOWN");
        long totalMonitored = healthyServices + downServices + routeRepository.countByHealthStatus("DEGRADED");
        int healthPercent = totalMonitored > 0 ? (int) ((healthyServices * 100) / totalMonitored) : 100;
        
        var stats = new DashboardStats(
                activeRoutes,
                0L, // TODO: requestsToday from analytics
                activeKeys,
                healthyServices,
                downServices,
                healthPercent,
                0 // routesTrend
        );
        model.addAttribute("stats", stats);
        
        // Recent routes (last 5)
        var recentRoutes = routeRepository.findByCreatedBy(user).stream()
                .limit(5)
                .toList();
        model.addAttribute("recentRoutes", recentRoutes);
        
        // Recent activities
        var recentActivities = activityLogService.getRecentByUser(user.getId(), 5).stream()
                .map(log -> new ActivityItem(
                        getActivityIcon(log.getAction()),
                        log.getDescription(),
                        formatTimeAgo(log.getCreatedAt())
                ))
                .toList();
        model.addAttribute("recentActivities", recentActivities);
        
        // User info for theme
        model.addAttribute("currentUser", user);
        
        return "dashboard";
    }

    private String getActivityIcon(String action) {
        return switch (action) {
            case "CREATE" -> "\uD83D\uDFE2";
            case "UPDATE" -> "\uD83D\uDFE1";
            case "DELETE" -> "\uD83D\uDD34";
            case "LOGIN" -> "\uD83D\uDD11";
            default -> "\uD83D\uDCDD";
        };
    }

    private String formatTimeAgo(java.time.LocalDateTime dateTime) {
        if (dateTime == null) return "";
        var now = java.time.LocalDateTime.now();
        var duration = java.time.Duration.between(dateTime, now);
        
        if (duration.toMinutes() < 1) return "just now";
        if (duration.toMinutes() < 60) return duration.toMinutes() + "m ago";
        if (duration.toHours() < 24) return duration.toHours() + "h ago";
        if (duration.toDays() < 7) return duration.toDays() + "d ago";
        return dateTime.toLocalDate().toString();
    }

    record DashboardStats(
            long activeRoutes,
            long requestsToday,
            long activeKeys,
            long healthyServices,
            long downServices,
            int healthPercent,
            int routesTrend
    ) {}

    record ActivityItem(String icon, String message, String timeAgo) {}
}
