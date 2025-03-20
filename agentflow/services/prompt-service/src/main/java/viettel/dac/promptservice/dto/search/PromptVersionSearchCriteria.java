package viettel.dac.promptservice.dto.search;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import viettel.dac.promptservice.model.enums.VersionStatus;

import java.util.List;

/**
 * Search criteria for prompt versions
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptVersionSearchCriteria {

    private String searchText; // Free text search
    private String templateId; // Template filter
    private List<VersionStatus> statuses; // Status filter
    private String createdBy; // Creator filter
    private String versionNumber; // Version number filter
    private String parameterName; // Has parameter filter
    private Boolean useExactMatch; // Use exact matching for text search
    private Boolean useFuzzyMatch; // Use fuzzy matching for text search
    private Float minScore; // Minimum relevance score
    private Integer minMajorVersion; // Minimum major version
    private Integer maxMajorVersion; // Maximum major version
}
