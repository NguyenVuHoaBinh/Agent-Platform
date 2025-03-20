package viettel.dac.promptservice.repository.elasticsearch;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;
import viettel.dac.promptservice.model.document.PromptExecutionDocument;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Repository for searching prompt executions in Elasticsearch
 */
@Repository
public interface PromptExecutionSearchRepository extends ElasticsearchRepository<PromptExecutionDocument, String> {

    /**
     * Find executions by version ID
     */
    Page<PromptExecutionDocument> findByVersionId(String versionId, Pageable pageable);

    /**
     * Find executions by template ID
     */
    Page<PromptExecutionDocument> findByTemplateId(String templateId, Pageable pageable);

    /**
     * Find executions by provider ID
     */
    Page<PromptExecutionDocument> findByProviderId(String providerId, Pageable pageable);

    /**
     * Find executions by model ID
     */
    Page<PromptExecutionDocument> findByModelId(String modelId, Pageable pageable);

    /**
     * Find executions by executed by
     */
    Page<PromptExecutionDocument> findByExecutedBy(String executedBy, Pageable pageable);

    /**
     * Find executions by date range
     */
    Page<PromptExecutionDocument> findByExecutedAtBetween(
            LocalDateTime fromDate, LocalDateTime toDate, Pageable pageable);

    /**
     * Find successful executions
     */
    Page<PromptExecutionDocument> findBySuccessfulTrue(Pageable pageable);

    /**
     * Find failed executions
     */
    Page<PromptExecutionDocument> findBySuccessfulFalse(Pageable pageable);

    /**
     * Get average response time for a version
     */
    @Query("{\"size\":0,\"aggs\":{\"avg_response_time\":{\"avg\":{\"field\":\"responseTimeMs\"}}}}")
    Map<String, Object> getAverageResponseTime(String versionId);

    /**
     * Get average token count for a version
     */
    @Query("{\"size\":0,\"aggs\":{\"avg_token_count\":{\"avg\":{\"field\":\"tokenCount\"}}}}")
    Map<String, Object> getAverageTokenCount(String versionId);

    /**
     * Get success rate for a version
     */
    @Query("{\"size\":0,\"aggs\":{\"success_rate\":{\"avg\":{\"field\":\"successful\"}}}}")
    Map<String, Object> getSuccessRate(String versionId);

    /**
     * Get execution count per day
     */
    @Query("{\"size\":0,\"aggs\":{\"executions_per_day\":{\"date_histogram\":{\"field\":\"executedAt\",\"calendar_interval\":\"day\"},\"aggs\":{\"count\":{\"value_count\":{\"field\":\"id\"}}}}}}")
    Map<String, Object> getExecutionsPerDay(String versionId);
}