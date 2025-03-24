package viettel.dac.promptservice.service.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import viettel.dac.promptservice.event.BatchJobCreationEvent;
import viettel.dac.promptservice.service.batch.BatchJobService;

/**
 * Listener for batch job related events.
 * This helps to break the circular dependency between services.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BatchJobEventListener {

    private final BatchJobService batchJobService;

    @EventListener
    public void handleBatchJobCreationEvent(BatchJobCreationEvent event) {
        log.debug("Handling batch job creation event");
        
        // Create the job using the service
        String jobId = batchJobService.createJob(event.getJobRequest());
        
        log.debug("Created batch job with ID: {}", jobId);
    }
} 