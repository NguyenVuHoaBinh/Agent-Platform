package viettel.dac.promptservice.config.elastic;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

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
     * Initialize indices immediately after construction
     */
    @PostConstruct
    public void init() {
        initializeIndices();
    }

    /**
     * Initialize indices when the application context is refreshed
     */
    @EventListener(ContextRefreshedEvent.class)
    public void onContextRefreshed() {
        initializeIndices();
    }

    /**
     * Perform the initialization of indices
     */
    private void initializeIndices() {
        try {
            log.info("Initializing Elasticsearch indices...");
            indexManager.createIndicesIfNotExist();
            log.info("Elasticsearch indices initialized successfully");
        } catch (Exception e) {
            log.error("Failed to initialize Elasticsearch indices", e);
        }
    }
}