package viettel.dac.promptservice.model.document;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.MultiField;
import viettel.dac.promptservice.model.entity.PromptTemplate;

/**
 * Elasticsearch document for prompt templates
 */
@Document(indexName = "#{@elasticsearchIndexConfig.promptTemplatesIndex}")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromptTemplateDocument extends BaseDocument {

    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "text_analyzer"),
            otherFields = {
                    @InnerField(suffix = "keyword", type = FieldType.Keyword)
            }
    )
    private String name;

    @Field(type = FieldType.Text, analyzer = "text_analyzer")
    private String description;

    @Field(type = FieldType.Keyword)
    private String category;

    @Field(type = FieldType.Keyword)
    private String projectId;

    @Field(type = FieldType.Keyword)
    private String createdBy;

    /**
     * Convert from entity to document
     */
    public static PromptTemplateDocument fromEntity(PromptTemplate template) {
        if (template == null) {
            return null;
        }

        PromptTemplateDocument document = PromptTemplateDocument.builder()
                .name(template.getName())
                .description(template.getDescription())
                .category(template.getCategory())
                .projectId(template.getProjectId())
                .createdBy(template.getCreatedBy())
                .build();

        // Set base document fields
        document.setId(template.getId());
        document.setCreatedAt(template.getCreatedAt());
        document.setUpdatedAt(template.getUpdatedAt());

        return document;
    }
}

