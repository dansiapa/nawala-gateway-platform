package id.nawala.platform.service.impl;

import id.nawala.platform.model.ActivityLog;
import id.nawala.platform.model.User;
import id.nawala.platform.repository.ActivityLogRepository;
import id.nawala.platform.service.ActivityLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ActivityLogServiceImpl implements ActivityLogService {

    private final ActivityLogRepository activityLogRepository;

    @Override
    public void log(User user, String action, String description, String ipAddress) {
        ActivityLog log = ActivityLog.builder()
                .user(user)
                .action(action)
                .description(description)
                .ipAddress(ipAddress)
                .build();
        activityLogRepository.save(log);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActivityLog> getRecentActivities(User user) {
        return activityLogRepository.findTop10ByUserOrderByCreatedAtDesc(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActivityLog> getRecentByUser(Long userId, int limit) {
        var pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        return activityLogRepository.findByUserId(userId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActivityLog> getSystemActivities() {
        return activityLogRepository.findTop20ByOrderByCreatedAtDesc();
    }
}
