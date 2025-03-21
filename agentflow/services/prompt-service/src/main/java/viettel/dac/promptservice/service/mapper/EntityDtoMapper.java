package viettel.dac.promptservice.service.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import viettel.dac.promptservice.dto.request.PromptParameterRequest;
import viettel.dac.promptservice.dto.request.PromptTemplateRequest;
import viettel.dac.promptservice.dto.request.PromptVersionRequest;
import viettel.dac.promptservice.dto.response.*;
import viettel.dac.promptservice.model.entity.PromptExecution;
import viettel.dac.promptservice.model.entity.PromptParameter;
import viettel.dac.promptservice.model.entity.PromptTemplate;
import viettel.dac.promptservice.model.entity.PromptVersion;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for mapping between entities and DTOs
 */
@Service
@RequiredArgsConstructor
public class EntityDtoMapper {

    /**
     * Convert PromptTemplate entity to response DTO
     */
    public PromptTemplateResponse toTemplateResponse(PromptTemplate template) {
        return PromptTemplateResponse.builder()
                .id(template.getId())
                .name(template.getName())
                .description(template.getDescription())
                .projectId(template.getProjectId())
                .category(template.getCategory())
                .createdBy(template.getCreatedBy())
                .createdAt(template.getCreatedAt())
                .updatedAt(template.getUpdatedAt())
                .hasPublishedVersion(template.hasPublishedVersion())
                .versionCount(template.getVersions().size())
                .build();
    }

    /**
     * Convert PromptTemplate entity to response DTO with versions
     */
    public PromptTemplateResponse toTemplateResponseWithVersions(PromptTemplate template) {
        PromptTemplateResponse response = toTemplateResponse(template);

        List<PromptVersionSummaryResponse> versionSummaries = template.getVersions().stream()
                .map(this::toVersionSummaryResponse)
                .collect(Collectors.toList());

        response.setVersions(versionSummaries);
        return response;
    }

    /**
     * Convert PromptVersion entity to summary response DTO
     */
    public PromptVersionSummaryResponse toVersionSummaryResponse(PromptVersion version) {
        return PromptVersionSummaryResponse.builder()
                .id(version.getId())
                .versionNumber(version.getVersionNumber())
                .status(version.getStatus())
                .createdBy(version.getCreatedBy())
                .createdAt(version.getCreatedAt())
                .parameterCount(version.getParameters().size())
                .build();
    }

    /**
     * Convert PromptVersion entity to response DTO
     */
    public PromptVersionResponse toVersionResponse(PromptVersion version) {
        PromptVersionResponse response = PromptVersionResponse.builder()
                .id(version.getId())
                .templateId(version.getTemplate().getId())
                .templateName(version.getTemplate().getName())
                .versionNumber(version.getVersionNumber())
                .content(version.getContent())
                .systemPrompt(version.getSystemPrompt())
                .createdBy(version.getCreatedBy())
                .createdAt(version.getCreatedAt())
                .updatedAt(version.getUpdatedAt())
                .status(version.getStatus())
                .build();

        if (version.getParentVersion() != null) {
            response.setParentVersionId(version.getParentVersion().getId());
        }

        List<PromptParameterResponse> parameterResponses = version.getParameters().stream()
                .map(this::toParameterResponse)
                .collect(Collectors.toList());

        response.setParameters(parameterResponses);
        return response;
    }

    /**
     * Convert PromptParameter entity to response DTO
     */
    public PromptParameterResponse toParameterResponse(PromptParameter parameter) {
        return PromptParameterResponse.builder()
                .id(parameter.getId())
                .name(parameter.getName())
                .description(parameter.getDescription())
                .parameterType(parameter.getParameterType())
                .defaultValue(parameter.getDefaultValue())
                .required(parameter.isRequired())
                .validationPattern(parameter.getValidationPattern())
                .build();
    }

    /**
     * Convert PromptExecution entity to response DTO
     */
    public PromptExecutionResponse toExecutionResponse(PromptExecution execution) {
        return PromptExecutionResponse.builder()
                .id(execution.getId())
                .versionId(execution.getVersion().getId())
                .templateId(execution.getVersion().getTemplate().getId())
                .providerId(execution.getProviderId())
                .modelId(execution.getModelId())
                .inputParameters(execution.getInputParameters())
                .response(execution.getRawResponse())
                .tokenCount(execution.getTokenCount())
                .inputTokens(execution.getInputTokens())
                .outputTokens(execution.getOutputTokens())
                .cost(execution.getCost())
                .responseTimeMs(execution.getResponseTimeMs())
                .executedAt(execution.getExecutedAt())
                .executedBy(execution.getExecutedBy())
                .status(execution.getStatus())
                .build();
    }

    /**
     * Convert PromptTemplateRequest to entity
     */
    public PromptTemplate toTemplateEntity(PromptTemplateRequest request) {
        return PromptTemplate.builder()
                .name(request.getName())
                .description(request.getDescription())
                .projectId(request.getProjectId())
                .category(request.getCategory())
                .build();
    }

    /**
     * Update PromptTemplate entity from request
     */
    public void updateTemplateFromRequest(PromptTemplate template, PromptTemplateRequest request) {
        template.setName(request.getName());
        template.setDescription(request.getDescription());
        template.setCategory(request.getCategory());
    }

    /**
     * Convert PromptVersionRequest to entity
     */
    public PromptVersion toVersionEntity(PromptVersionRequest request) {
        PromptVersion version = PromptVersion.builder()
                .versionNumber(request.getVersionNumber())
                .content(request.getContent())
                .systemPrompt(request.getSystemPrompt())
                .build();

        return version;
    }

    /**
     * Convert PromptParameterRequest to entity
     */
    public PromptParameter toParameterEntity(PromptParameterRequest request) {
        return PromptParameter.builder()
                .name(request.getName())
                .description(request.getDescription())
                .parameterType(request.getParameterType())
                .defaultValue(request.getDefaultValue())
                .required(request.isRequired())
                .validationPattern(request.getValidationPattern())
                .build();
    }

    /**
     * Convert Page of entities to PageResponse of DTOs
     */
    public <T, R> PageResponse<R> toPageResponse(Page<T> page, java.util.function.Function<T, R> mapper) {
        List<R> content = page.getContent().stream()
                .map(mapper)
                .collect(Collectors.toList());

        return PageResponse.<R>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }
}