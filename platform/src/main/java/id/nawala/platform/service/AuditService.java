package id.nawala.platform.service;

import id.nawala.platform.model.AuditLog;
import java.util.List;

/**
 * Audit trail service for tracking all significant operations.
 */
public interface AuditService {

    void log(Long userId, String username, String action, String resourceType,
             Long resourceId, String details, String ipAddress);

    List<AuditLog> getByUser(Long userId);

    List<AuditLog> getRecent(int limit);

    List<AuditLog> getByResource(String resourceType, Long resourceId);
}
