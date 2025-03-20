package viettel.dac.promptservice.service.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import viettel.dac.promptservice.model.entity.PromptVersion;
import viettel.dac.promptservice.model.enums.VersionStatus;

/**
 * Event published when a version's status changes.
 */
@Getter
public class VersionStatusChangeEvent extends ApplicationEvent {
    private final VersionStatus oldStatus;
    private final VersionStatus newStatus;

    public VersionStatusChangeEvent(PromptVersion version, VersionStatus oldStatus, VersionStatus newStatus) {
        super(version);
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
    }

    /**
     * Get the version that had its status changed.
     */
    public PromptVersion getVersion() {
        return (PromptVersion) getSource();
    }
}