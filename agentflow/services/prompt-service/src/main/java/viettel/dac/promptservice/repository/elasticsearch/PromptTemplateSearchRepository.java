package viettel.dac.promptservice.repository.elasticsearch;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;
import viettel.dac.promptservice.model.document.PromptTemplateDocument;

import java.util.List;

/**
 * Repository for searching prompt templates in Elasticsearch
 */
@Repository
public interface PromptTemplateSearchRepository extends ElasticsearchRepository<PromptTemplateDocument, String> {

    /**
     * Find templates by name or description (text search)
     */
    Page<PromptTemplateDocument> findByNameContainingOrDescriptionContaining(
            String name, String description, Pageable pageable);

    /**
     * Find templates by project ID
     */
    Page<PromptTemplateDocument> findByProjectId(String projectId, Pageable pageable);

    /**
     * Find templates by category
     */
    Page<PromptTemplateDocument> findByCategory(String category, Pageable pageable);

    /**
     * Find templates by creator
     */
    Page<PromptTemplateDocument> findByCreatedBy(String createdBy, Pageable pageable);

    /**
     * Find templates that have published versions
     */
    Page<PromptTemplateDocument> findByHasPublishedVersionTrue(Pageable pageable);

    /**
     * Find templates with minimum version count
     */
    @Query("{\"range\": {\"versionCount\": {\"gte\": ?0}}}")
    Page<PromptTemplateDocument> findByMinimumVersionCount(int minCount, Pageable pageable);

    /**
     * Find templates by multiple categories
     */
    Page<PromptTemplateDocument> findByCategoryIn(List<String> categories, Pageable pageable);

    /**
     * Get distinct categories
     */
    @Query("{\"size\": 0, \"aggs\": {\"categories\": {\"terms\": {\"field\": \"category\", \"size\": 1000}}}}")
    List<String> findDistinctCategories();
}