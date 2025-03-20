package viettel.dac.promptservice.service.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import viettel.dac.promptservice.dto.search.PromptTemplateSearchCriteria;
import viettel.dac.promptservice.model.document.PromptTemplateDocument;
import viettel.dac.promptservice.model.entity.PromptTemplate;
import viettel.dac.promptservice.repository.elasticsearch.PromptTemplateSearchRepository;
import viettel.dac.promptservice.repository.jpa.PromptTemplateRepository;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class PromptSearchService {

    private final PromptTemplateSearchRepository searchRepository;
    private final DocumentConversionService conversionService;
    private final ElasticsearchQueryBuilder queryBuilder;
    private final ElasticsearchClient elasticsearchClient;
    private final PromptTemplateRepository templateRepository;

    @Value("${spring.elasticsearch.index.prompt-templates:prompt_templates}")
    private String promptTemplatesIndex;

    @Value("${elasticsearch.indexing.batch-size:100}")
    private int batchSize;

    /**
     * Index a prompt template in Elasticsearch with retry mechanism
     *
     * @param template The template to index
     */
    @Retryable(
            value = {IOException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void indexPromptTemplate(PromptTemplate template) {
        log.debug("Indexing template in Elasticsearch: {}", template.getId());
        try {
            PromptTemplateDocument document = conversionService.convertTemplateToDocument(template);
            searchRepository.save(document);
            log.debug("Successfully indexed template: {}", template.getId());
        } catch (Exception e) {
            log.error("Error indexing template {}: {}", template.getId(), e.getMessage());
            throw new RuntimeException("Failed to index template", e);
        }
    }

    /**
     * Bulk index multiple templates for efficiency
     *
     * @param templates List of templates to index
     */
    @Retryable(
            value = {IOException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void bulkIndexTemplates(List<PromptTemplate> templates) {
        if (templates == null || templates.isEmpty()) {
            return;
        }

        log.debug("Bulk indexing {} templates", templates.size());

        try {
            List<PromptTemplateDocument> documents = templates.stream()
                    .map(conversionService::convertTemplateToDocument)
                    .collect(Collectors.toList());

            // Process in batches for better performance
            for (int i = 0; i < documents.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, documents.size());
                List<PromptTemplateDocument> batch = documents.subList(i, endIndex);

                searchRepository.saveAll(batch);
                log.debug("Indexed batch of {} documents ({}-{})", batch.size(), i, endIndex-1);
            }

            log.debug("Successfully bulk indexed {} templates", templates.size());
        } catch (Exception e) {
            log.error("Error during bulk indexing: {}", e.getMessage());
            throw new RuntimeException("Failed to bulk index templates", e);
        }
    }

    /**
     * Search for templates using Elasticsearch with retry mechanism
     *
     * @param criteria Search criteria
     * @param pageable Pagination parameters
     * @return Page of matching templates
     */
    @Retryable(
            value = {IOException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public Page<PromptTemplate> searchTemplates(PromptTemplateSearchCriteria criteria, Pageable pageable) {
        log.debug("Searching templates in Elasticsearch with criteria: {}", criteria);

        try {
            // Build and execute search request
            var searchRequest = queryBuilder.buildTemplateSearchRequest(criteria, pageable, promptTemplatesIndex);
            SearchResponse<PromptTemplateDocument> searchResponse =
                    elasticsearchClient.search(searchRequest, PromptTemplateDocument.class);

            if (searchResponse.hits().total().value() == 0) {
                return new PageImpl<>(Collections.emptyList(), pageable, 0);
            }

            // Extract document IDs from search hits
            List<String> templateIds = searchResponse.hits().hits().stream()
                    .map(Hit::id)
                    .collect(Collectors.toList());

            // Fetch full entities from the database
            List<PromptTemplate> templates = templateRepository.findAllById(templateIds);

            // Create a map for efficient lookup
            Map<String, PromptTemplate> templateMap = templates.stream()
                    .collect(Collectors.toMap(PromptTemplate::getId, Function.identity()));

            // Sort templates according to search result order
            List<PromptTemplate> sortedTemplates = templateIds.stream()
                    .map(templateMap::get)
                    .filter(t -> t != null)
                    .collect(Collectors.toList());

            // Create paginated result
            long totalHits = searchResponse.hits().total().value();
            return new PageImpl<>(sortedTemplates, pageable, totalHits);

        } catch (IOException e) {
            log.error("Error searching templates in Elasticsearch", e);
            throw new RuntimeException("Failed to search templates", e);
        }
    }

    /**
     * Delete a template from the Elasticsearch index with retry mechanism
     *
     * @param templateId The template ID to delete
     */
    @Retryable(
            value = {IOException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void deleteTemplateIndex(String templateId) {
        log.debug("Deleting template from Elasticsearch index: {}", templateId);
        try {
            searchRepository.deleteById(templateId);
            log.debug("Successfully deleted template from index: {}", templateId);
        } catch (Exception e) {
            log.error("Error deleting template {} from index: {}", templateId, e.getMessage());
            throw new RuntimeException("Failed to delete template from index", e);
        }
    }

    /**
     * Check if the Elasticsearch index exists and is available
     *
     * @return true if index is available
     */
    public boolean isIndexAvailable() {
        try {
            return elasticsearchClient.indices().exists(r -> r.index(promptTemplatesIndex)).value();
        } catch (Exception e) {
            log.error("Error checking Elasticsearch index availability: {}", e.getMessage());
            return false;
        }
    }
}