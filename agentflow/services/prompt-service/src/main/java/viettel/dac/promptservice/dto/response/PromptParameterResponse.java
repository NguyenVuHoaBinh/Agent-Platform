package viettel.dac.promptservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import viettel.dac.promptservice.model.enums.ParameterType;

/**
 * DTO for returning prompt parameter information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PromptParameterResponse {

    private String id;
    private String name;
    private String description;
    private ParameterType parameterType;
    private String defaultValue;
    private boolean required;
    private String validationPattern;
}