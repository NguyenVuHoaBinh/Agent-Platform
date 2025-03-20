package viettel.dac.promptservice.model.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import viettel.dac.promptservice.model.entity.PromptVersion;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Elasticsearch document for prompt versions
 */
@Document(indexName = "#{@elasticsearchIndexConfig.promptVersionsIndex}")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromptVersionDocument extends BaseDocument {

    @Field(type = FieldType.Keyword)
    private String templateId;

    @Field(type = FieldType.Keyword)
    private String versionNumber;

    @Field(type = FieldType.Text, analyzer = "prompt_analyzer")
    private String content;

    @Field(type = FieldType.Keyword)
    private String status;

    @Field(type = FieldType.Keyword)
    private String createdBy;

    @Field(type = FieldType.Nested)
    private List<PromptParameterDocument> parameters;

    /**
     * Convert from entity to document
     */
    public static PromptVersionDocument fromEntity(PromptVersion version) {
        if (version == null) {
            return null;
        }

        PromptVersionDocument document = PromptVersionDocument.builder()
                .templateId(version.getTemplate().getId())
                .versionNumber(version.getVersionNumber())
                .content(version.getContent())
                .status(version.getStatus().name())
                .createdBy(version.getCreatedBy())
                .build();

        // Set base document fields
        document.setId(version.getId());
        document.setCreatedAt(version.getCreatedAt());
        document.setUpdatedAt(version.getUpdatedAt());

        // Convert parameters
        if (version.getParameters() != null && !version.getParameters().isEmpty()) {
            document.setParameters(version.getParameters().stream()
                    .map(PromptParameterDocument::fromEntity)
                    .collect(Collectors.toList()));
        }

        return document;
    }
}