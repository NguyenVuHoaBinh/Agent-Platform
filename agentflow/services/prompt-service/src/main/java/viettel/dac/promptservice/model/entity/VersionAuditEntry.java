package viettel.dac.promptservice.model.entity;

import jakarta.persistence.*;
import lombok.*;
import viettel.dac.promptservice.model.enums.VersionStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "version_audit_logs", indexes = {
        @Index(name = "idx_audit_version", columnList = "version_id"),
        @Index(name = "idx_audit_performed_at", columnList = "performed_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VersionAuditEntry extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "version_id", nullable = false)
    private PromptVersion version;

    @Column(name = "action_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private AuditActionType actionType;

    @Column(name = "performed_by", nullable = false, length = 100)
    private String performedBy;

    @Column(name = "performed_at", nullable = false)
    private LocalDateTime performedAt;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @Column(name = "previous_status", length = 50)
    @Enumerated(EnumType.STRING)
    private VersionStatus previousStatus;

    @Column(name = "new_status", length = 50)
    @Enumerated(EnumType.STRING)
    private VersionStatus newStatus;

    @Column(name = "reference_version_id", length = 36)
    private String referenceVersionId;

    public enum AuditActionType {
        CREATED,
        STATUS_CHANGED,
        CONTENT_UPDATED,
        PARAMETERS_UPDATED,
        ROLLBACK,
        BRANCHED
    }
}