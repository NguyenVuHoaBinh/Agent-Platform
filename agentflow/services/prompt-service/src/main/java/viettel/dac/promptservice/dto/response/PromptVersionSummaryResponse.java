package viettel.dac.promptservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import viettel.dac.promptservice.model.enums.VersionStatus;

import java.time.LocalDateTime;

/**
 * DTO for returning summarized prompt version information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PromptVersionSummaryResponse {

    private String id;
    private String versionNumber;
    private VersionStatus status;
    private String createdBy;
    private LocalDateTime createdAt;
    private int parameterCount;
}