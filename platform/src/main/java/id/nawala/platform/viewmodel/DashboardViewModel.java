package id.nawala.platform.viewmodel;

import id.nawala.platform.model.ActivityLog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardViewModel {

    private String username;
    private String fullName;
    private String email;
    private String role;
    private String initials;
    private LocalDateTime memberSince;
    private LocalDateTime lastLogin;
    private long totalUsers;
    private long activeUsers;
    private long totalRoutes;
    private long activeRoutes;
    private long activeApiKeys;
    private List<ActivityLog> recentActivities;

    // Health Monitor stats
    private int healthUp;
    private int healthDown;
    private int healthDegraded;
    private int healthTotal;

    // Anomaly Detection stats
    private long unresolvedThreats;
    private long criticalThreats;
    private long blockedSources;
    private long threats24h;
}

