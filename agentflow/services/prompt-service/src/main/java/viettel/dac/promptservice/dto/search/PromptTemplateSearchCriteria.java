package viettel.dac.promptservice.dto.search;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Search criteria for prompt templates
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptTemplateSearchCriteria {

    private String searchText; // Free text search
    private String projectId; // Project filter
    private String category; // Category filter
    private Set<String> categories; // Multiple categories
    private String createdBy; // Creator filter
    private LocalDateTime fromDate; // Created after
    private LocalDateTime toDate; // Created before
    private Boolean hasPublishedVersion; // Has published version filter
    private Integer minVersionCount; // Minimum number of versions
    private Boolean useExactMatch; // Use exact matching for text search
    private Boolean useFuzzyMatch; // Use fuzzy matching for text search
    private Float minScore; // Minimum relevance score
}
