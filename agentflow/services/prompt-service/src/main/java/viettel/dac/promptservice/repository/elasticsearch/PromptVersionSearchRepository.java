package viettel.dac.promptservice.repository.elasticsearch;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;
import viettel.dac.promptservice.model.document.PromptVersionDocument;

import java.util.List;

/**
 * Repository for searching prompt versions in Elasticsearch
 */
@Repository
public interface PromptVersionSearchRepository extends ElasticsearchRepository<PromptVersionDocument, String> {

    /**
     * Find versions by template ID
     */
    Page<PromptVersionDocument> findByTemplateId(String templateId, Pageable pageable);

    /**
     * Find versions by content (text search)
     */
    Page<PromptVersionDocument> findByContentContaining(String content, Pageable pageable);

    /**
     * Find versions by status
     */
    Page<PromptVersionDocument> findByStatus(String status, Pageable pageable);

    /**
     * Find versions by multiple statuses
     */
    Page<PromptVersionDocument> findByStatusIn(List<String> statuses, Pageable pageable);

    /**
     * Find versions by version number
     */
    Page<PromptVersionDocument> findByVersionNumber(String versionNumber, Pageable pageable);

    /**
     * Find versions by parameter name
     */
    Page<PromptVersionDocument> findByParameterNamesContaining(String parameterName, Pageable pageable);

    /**
     * Find latest version for a template
     */
    @Query("{\"bool\": {\"must\": [{\"term\": {\"templateId\": \"?0\"}}], \"sort\": [{\"createdAt\": {\"order\": \"desc\"}}], \"size\": 1}}")
    PromptVersionDocument findLatestVersionForTemplate(String templateId);

    /**
     * Find latest published version for a template
     */
    @Query("{\"bool\": {\"must\": [{\"term\": {\"templateId\": \"?0\"}}, {\"term\": {\"status\": \"PUBLISHED\"}}], \"sort\": [{\"createdAt\": {\"order\": \"desc\"}}], \"size\": 1}}")
    PromptVersionDocument findLatestPublishedVersionForTemplate(String templateId);
}