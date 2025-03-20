package viettel.dac.promptservice.model.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import viettel.dac.promptservice.model.entity.PromptParameter;

/**
 * Elasticsearch nested document for prompt parameters
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromptParameterDocument {

    @Field(type = FieldType.Keyword)
    private String name;

    @Field(type = FieldType.Text)
    private String description;

    @Field(type = FieldType.Keyword)
    private String parameterType;

    @Field(type = FieldType.Boolean)
    private boolean required;

    /**
     * Convert from entity to document
     */
    public static PromptParameterDocument fromEntity(PromptParameter parameter) {
        if (parameter == null) {
            return null;
        }

        return PromptParameterDocument.builder()
                .name(parameter.getName())
                .description(parameter.getDescription())
                .parameterType(parameter.getParameterType().name())
                .required(parameter.isRequired())
                .build();
    }
}
