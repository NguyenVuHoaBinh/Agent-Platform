package viettel.dac.promptservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * DTO for executing a prompt version
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptExecutionRequest {

    @NotBlank(message = "Version ID is required")
    private String versionId;

    @NotBlank(message = "Provider ID is required")
    private String providerId;

    @NotBlank(message = "Model ID is required")
    private String modelId;

    @Builder.Default
    private Map<String, Object> parameters = new HashMap<>();
}