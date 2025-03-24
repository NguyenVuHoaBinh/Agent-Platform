package viettel.dac.promptservice.service.elasticsearch;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import viettel.dac.promptservice.model.document.PromptExecutionDocument;
import viettel.dac.promptservice.model.document.PromptParameterDocumentConverter;
import viettel.dac.promptservice.model.document.PromptTemplateDocument;
import viettel.dac.promptservice.model.document.PromptVersionDocument;
import viettel.dac.promptservice.model.entity.PromptExecution;
import viettel.dac.promptservice.model.entity.PromptParameter;
import viettel.dac.promptservice.model.entity.PromptTemplate;
import viettel.dac.promptservice.model.entity.PromptVersion;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for converting between JPA entities and Elasticsearch documents
 */
@Service
@RequiredArgsConstructor
public class DocumentConversionService {

    /**
     * Convert a PromptTemplate entity to an Elasticsearch document
     */
    public PromptTemplateDocument convertTemplateToDocument(PromptTemplate template) {
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

        // Add additional search fields
        document.setHasPublishedVersion(template.hasPublishedVersion());
        document.setVersionCount(template.getVersions().size());

        // Add keywords for faceted search
        document.addKeyword("project", template.getProjectId());
        document.addKeyword("category", template.getCategory());
        document.addKeyword("creator", template.getCreatedBy());

        return document;
    }

    /**
     * Convert a PromptVersion entity to an Elasticsearch document
     */
    public PromptVersionDocument convertVersionToDocument(PromptVersion version) {
        if (version == null) {
            return null;
        }

        PromptVersionDocument document = PromptVersionDocument.builder()
                .templateId(version.getTemplate().getId())
                .templateName(version.getTemplate().getName())
                .versionNumber(version.getVersionNumber())
                .content(version.getContent())
                .status(version.getStatus().name())
                .createdBy(version.getCreatedBy())
                .majorVersion(version.getMajorVersion())
                .minorVersion(version.getMinorVersion())
                .patchVersion(version.getPatchVersion())
                .build();

        // Set base document fields
        document.setId(version.getId());
        document.setCreatedAt(version.getCreatedAt());
        document.setUpdatedAt(version.getUpdatedAt());

        // Convert parameters
        if (version.getParameters() != null && !version.getParameters().isEmpty()) {
            document.setParameters(version.getParameters().stream()
                    .map(PromptParameterDocumentConverter::fromEntity)
                    .collect(Collectors.toList()));

            // Extract parameter names for search
            document.setParameterNames(version.getParameters().stream()
                    .map(PromptParameter::getName)
                    .collect(Collectors.toSet()));
        }

        // Add extracted content tokens for search
        document.setContentTokens(tokenizeContent(version.getContent()));

        return document;
    }

    /**
     * Convert a PromptExecution entity to an Elasticsearch document
     */
    public PromptExecutionDocument convertExecutionToDocument(PromptExecution execution) {
        if (execution == null) {
            return null;
        }

        PromptExecutionDocument document = PromptExecutionDocument.builder()
                .versionId(execution.getVersion().getId())
                .templateId(execution.getVersion().getTemplate().getId())
                .providerId(execution.getProviderId())
                .modelId(execution.getModelId())
                .inputParameters(execution.getInputParameters())
                .tokenCount(execution.getTokenCount())
                .inputTokens(execution.getInputTokens())
                .outputTokens(execution.getOutputTokens())
                .cost(execution.getCost() != null ? execution.getCost().doubleValue() : null)
                .responseTimeMs(execution.getResponseTimeMs())
                .executedAt(execution.getExecutedAt())
                .executedBy(execution.getExecutedBy())
                .status(execution.getStatus().name())
                .build();

        // Set base document fields
        document.setId(execution.getId());
        document.setCreatedAt(execution.getCreatedAt());
        document.setUpdatedAt(execution.getUpdatedAt());

        // Add metrics for aggregations
        document.setSuccessful(execution.isSuccessful());
        document.setYear(execution.getExecutedAt().getYear());
        document.setMonth(execution.getExecutedAt().getMonthValue());
        document.setDay(execution.getExecutedAt().getDayOfMonth());

        return document;
    }

    /**
     * Convert a list of PromptTemplate entities to Elasticsearch documents
     */
    public List<PromptTemplateDocument> convertTemplatesToDocuments(List<PromptTemplate> templates) {
        return templates.stream()
                .map(this::convertTemplateToDocument)
                .collect(Collectors.toList());
    }

    /**
     * Convert a list of PromptVersion entities to Elasticsearch documents
     */
    public List<PromptVersionDocument> convertVersionsToDocuments(List<PromptVersion> versions) {
        return versions.stream()
                .map(this::convertVersionToDocument)
                .collect(Collectors.toList());
    }

    /**
     * Convert a list of PromptExecution entities to Elasticsearch documents
     */
    public List<PromptExecutionDocument> convertExecutionsToDocuments(List<PromptExecution> executions) {
        return executions.stream()
                .map(this::convertExecutionToDocument)
                .collect(Collectors.toList());
    }

    /**
     * Tokenize content for improved search
     */
    private List<String> tokenizeContent(String content) {
        if (content == null || content.isEmpty()) {
            return List.of();
        }

        // Simple whitespace tokenization with lowercasing and punctuation removal
        return List.of(content.toLowerCase()
                .replaceAll("[^a-zA-Z0-9\\s]", " ")
                .split("\\s+"));
    }
}