package id.nawala.platform.service;

import id.nawala.platform.model.ActivityLog;
import id.nawala.platform.model.User;

import java.util.List;

public interface ActivityLogService {

    void log(User user, String action, String description, String ipAddress);

    List<ActivityLog> getRecentActivities(User user);

    List<ActivityLog> getSystemActivities();
}
