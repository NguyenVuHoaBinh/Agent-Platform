package viettel.dac.identityservice.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Criteria for searching audit logs
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuditLogSearchCriteria {

    private String userId;
    private String eventType;
    private String resourceType;
    private String resourceId;
    private String action;
    private String ipAddress;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String searchTerm;
}