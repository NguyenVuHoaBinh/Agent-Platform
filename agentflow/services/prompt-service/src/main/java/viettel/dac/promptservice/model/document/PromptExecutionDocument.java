package viettel.dac.promptservice.model.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import viettel.dac.promptservice.model.entity.PromptExecution;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Elasticsearch document for prompt executions
 */
@Document(indexName = "#{@elasticsearchIndexConfig.promptExecutionsIndex}")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromptExecutionDocument extends BaseDocument {

    @Field(type = FieldType.Keyword)
    private String versionId;

    @Field(type = FieldType.Keyword)
    private String providerId;

    @Field(type = FieldType.Keyword)
    private String modelId;

    @Field(type = FieldType.Object)
    private Map<String, Object> inputParameters;

    @Field(type = FieldType.Integer)
    private Integer tokenCount;

    @Field(type = FieldType.Integer)
    private Integer inputTokens;

    @Field(type = FieldType.Integer)
    private Integer outputTokens;

    @Field(type = FieldType.Float)
    private BigDecimal cost;

    @Field(type = FieldType.Long)
    private Long responseTimeMs;

    @Field(type = FieldType.Date)
    private LocalDateTime executedAt;

    @Field(type = FieldType.Keyword)
    private String executedBy;

    @Field(type = FieldType.Keyword)
    private String status;

    /**
     * Convert from entity to document
     */
    public static PromptExecutionDocument fromEntity(PromptExecution execution) {
        if (execution == null) {
            return null;
        }

        PromptExecutionDocument document = PromptExecutionDocument.builder()
                .versionId(execution.getVersion().getId())
                .providerId(execution.getProviderId())
                .modelId(execution.getModelId())
                .inputParameters(execution.getInputParameters())
                .tokenCount(execution.getTokenCount())
                .inputTokens(execution.getInputTokens())
                .outputTokens(execution.getOutputTokens())
                .cost(execution.getCost())
                .responseTimeMs(execution.getResponseTimeMs())
                .executedAt(execution.getExecutedAt())
                .executedBy(execution.getExecutedBy())
                .status(execution.getStatus().name())
                .build();

        // Set base document fields
        document.setId(execution.getId());
        document.setCreatedAt(execution.getCreatedAt());
        document.setUpdatedAt(execution.getUpdatedAt());

        return document;
    }
}
