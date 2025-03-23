package viettel.dac.promptservice.model.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.MultiField;

import java.util.HashMap;
import java.util.Map;

/**
 * Enhanced Elasticsearch document for prompt templates with improved search capabilities
 */
@Document(indexName = "#{@elasticsearchIndexConfig.promptTemplatesIndex}")
@Getter
@Setter
@SuperBuilder
public class PromptTemplateDocument extends BaseDocument {

    // Enhanced field mappings for better search
    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "text_analyzer"),
            otherFields = {
                    @InnerField(suffix = "keyword", type = FieldType.Keyword),
                    @InnerField(suffix = "autocomplete", type = FieldType.Text, analyzer = "autocomplete_analyzer")
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

    // Additional fields for improved search and filtering
    @Field(type = FieldType.Boolean)
    private boolean hasPublishedVersion;

    @Field(type = FieldType.Integer)
    private int versionCount;

    @Field(type = FieldType.Object)
    @Builder.Default
    private Map<String, String> keywords = new HashMap<>();

    /**
     * Add a keyword for faceted search
     */
    public void addKeyword(String key, String value) {
        if (key != null && value != null) {
            keywords.put(key, value);
        }
    }
}