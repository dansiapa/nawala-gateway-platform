package id.nawala.platform.service.impl;

import id.nawala.platform.model.AuditLog;
import id.nawala.platform.repository.AuditLogRepository;
import id.nawala.platform.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository auditLogRepository;

    @Override
    @Async
    @Transactional
    public void log(Long userId, String username, String action, String resourceType,
                    Long resourceId, String details, String ipAddress) {
        AuditLog entry = AuditLog.builder()
                .userId(userId).username(username).action(action)
                .resourceType(resourceType).resourceId(resourceId)
                .details(details).ipAddress(ipAddress)
                .build();
        auditLogRepository.save(entry);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLog> getByUser(Long userId) {
        return auditLogRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLog> getRecent(int limit) {
        return auditLogRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, limit)).getContent();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLog> getByResource(String resourceType, Long resourceId) {
        return auditLogRepository.findByResourceTypeAndResourceIdOrderByCreatedAtDesc(resourceType, resourceId);
    }
}
