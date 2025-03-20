package viettel.dac.promptservice.dto.search;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Search criteria for prompt executions
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptExecutionSearchCriteria {

    private String versionId; // Version filter
    private String templateId; // Template filter
    private String providerId; // Provider filter
    private String modelId; // Model filter
    private String executedBy; // User filter
    private LocalDateTime fromDate; // Executed after
    private LocalDateTime toDate; // Executed before
    private Boolean successful; // Successful execution filter
    private Integer minTokenCount; // Minimum token count
    private Integer maxTokenCount; // Maximum token count
}
