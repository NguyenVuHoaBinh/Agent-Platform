package viettel.dac.promptservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import viettel.dac.promptservice.model.enums.ParameterType;

/**
 * DTO for creating or updating a prompt parameter
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptParameterRequest {

    @NotBlank(message = "Parameter name is required")
    @Size(min = 1, max = 100, message = "Parameter name must be between 1 and 100 characters")
    private String name;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @NotNull(message = "Parameter type is required")
    private ParameterType parameterType;

    private String defaultValue;

    @Builder.Default
    private boolean required = false;

    private String validationPattern;
}