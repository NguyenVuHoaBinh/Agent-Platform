package viettel.dac.promptservice.model.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.List;
import java.util.Set;
import java.util.HashSet;

/**
 * Enhanced Elasticsearch document for prompt versions with improved search
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

    @Field(type = FieldType.Text, analyzer = "text_analyzer")
    private String templateName;

    @Field(type = FieldType.Keyword)
    private String versionNumber;

    // Split version into components for range queries
    @Field(type = FieldType.Integer)
    private int majorVersion;

    @Field(type = FieldType.Integer)
    private int minorVersion;

    @Field(type = FieldType.Integer)
    private int patchVersion;

    @Field(type = FieldType.Text, analyzer = "prompt_analyzer")
    private String content;

    // Tokenized content for better search
    @Field(type = FieldType.Keyword)
    private List<String> contentTokens;

    @Field(type = FieldType.Keyword)
    private String status;

    @Field(type = FieldType.Keyword)
    private String createdBy;

    @Field(type = FieldType.Nested)
    private List<PromptParameterDocument> parameters;

    // Parameter names for easier filtering
    @Field(type = FieldType.Keyword)
    @Builder.Default
    private Set<String> parameterNames = new HashSet<>();
}