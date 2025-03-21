package viettel.dac.promptservice.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO for creating or updating a prompt version
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptVersionRequest {

    @NotBlank(message = "Template ID is required")
    private String templateId;

    @Pattern(regexp = "^\\d+\\.\\d+\\.\\d+$", message = "Version number must follow semver format (major.minor.patch)")
    private String versionNumber;

    @NotBlank(message = "Content is required")
    private String content;

    private String systemPrompt;

    private String parentVersionId;

    @Valid
    @Builder.Default
    private List<PromptParameterRequest> parameters = new ArrayList<>();
}