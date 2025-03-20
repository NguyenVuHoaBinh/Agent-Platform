package viettel.dac.promptservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO for returning prompt template information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PromptTemplateResponse {

    private String id;
    private String name;
    private String description;
    private String projectId;
    private String category;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean hasPublishedVersion;
    private int versionCount;

    @Builder.Default
    private List<PromptVersionSummaryResponse> versions = new ArrayList<>();
}