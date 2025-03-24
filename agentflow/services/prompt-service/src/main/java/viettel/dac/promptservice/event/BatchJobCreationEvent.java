package viettel.dac.promptservice.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import viettel.dac.promptservice.dto.request.BatchJobRequest;

/**
 * Event triggered when a batch job needs to be created.
 * This helps to break circular dependencies between services.
 */
@Getter
public class BatchJobCreationEvent extends ApplicationEvent {
    
    private final BatchJobRequest jobRequest;

    public BatchJobCreationEvent(Object source, BatchJobRequest jobRequest) {
        super(source);
        this.jobRequest = jobRequest;
    }
} 