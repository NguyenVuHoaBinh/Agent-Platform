package viettel.dac.promptservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import viettel.dac.promptservice.model.enums.VersionStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO for returning prompt version information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PromptVersionResponse {

    private String id;
    private String templateId;
    private String templateName;
    private String versionNumber;
    private String content;
    private String systemPrompt;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private VersionStatus status;
    private String parentVersionId;

    @Builder.Default
    private List<PromptParameterResponse> parameters = new ArrayList<>();
}