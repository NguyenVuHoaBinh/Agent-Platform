package viettel.dac.promptservice.model.document;

import viettel.dac.promptservice.model.entity.PromptParameter;

/**
 * Utility class to convert PromptParameter entities to document objects
 */
public class PromptParameterDocumentConverter {

    /**
     * Convert a parameter entity to document
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