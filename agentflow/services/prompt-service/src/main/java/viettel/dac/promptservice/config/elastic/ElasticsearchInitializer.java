package viettel.dac.promptservice.config.elastic;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Component to initialize Elasticsearch indices on application startup
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Profile("!test") // Skip during tests
public class ElasticsearchInitializer {

    private final ElasticsearchIndexManager indexManager;

    /**
     * Initialize indices when the application is ready
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initializeIndices() {
        try {
            log.info("Initializing Elasticsearch indices...");
            indexManager.createIndicesIfNotExist();
            log.info("Elasticsearch indices initialized successfully");
        } catch (Exception e) {
            log.error("Failed to initialize Elasticsearch indices", e);
        }
    }
}