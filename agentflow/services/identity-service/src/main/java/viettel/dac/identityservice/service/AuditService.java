package viettel.dac.identityservice.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import viettel.dac.identityservice.entity.AuditLog;

import java.util.List;
import java.util.Map;

/**
 * Service for audit logging and retrieval
 */
public interface AuditService {

    /**
     * Log an authentication-related event
     *
     * @param userId User ID (can be null for failed authentication)
     * @param eventType Type of event (e.g., "authentication.success", "authentication.failed")
     * @param ipAddress Client IP address
     * @param additionalData Additional event data
     */
    void logAuthEvent(String userId, String eventType, String ipAddress, Map<String, Object> additionalData);

    /**
     * Log a resource-related event
     *
     * @param resourceType Type of resource (e.g., "user", "organization")
     * @param resourceId Resource ID
     * @param action Action performed (e.g., "create", "update", "delete")
     * @param additionalData Additional event data
     */
    void logResourceEvent(String resourceType, String resourceId, String action, Map<String, Object> additionalData);

    /**
     * Search audit logs based on criteria
     *
     * @param criteria Search criteria
     * @param pageable Pagination information
     * @return Page of matching audit logs
     */
    Page<AuditLog> searchAuditLogs(AuditLogSearchCriteria criteria, Pageable pageable);

    /**
     * Get user activity timeline
     *
     * @param userId User ID
     * @param days Number of days to include
     * @return List of audit logs for the user
     */
    List<AuditLog> getUserActivityTimeline(String userId, int days);

    /**
     * Get count of events by type
     *
     * @param userId User ID
     * @param days Number of days to include
     * @return Map of event types to counts
     */
    Map<String, Long> getEventTypeCounts(String userId, int days);
}