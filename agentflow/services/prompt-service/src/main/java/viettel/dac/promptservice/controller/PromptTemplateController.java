package viettel.dac.promptservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import viettel.dac.promptservice.dto.request.PromptTemplateRequest;
import viettel.dac.promptservice.dto.response.ErrorResponse;
import viettel.dac.promptservice.dto.response.PageResponse;
import viettel.dac.promptservice.dto.response.PromptTemplateResponse;
import viettel.dac.promptservice.dto.search.PromptTemplateSearchCriteria;
import viettel.dac.promptservice.model.entity.PromptTemplate;
import viettel.dac.promptservice.service.PromptManagementService;
import viettel.dac.promptservice.service.mapper.EntityDtoMapper;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/templates")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Prompt Templates", description = "API endpoints for managing prompt templates")
public class PromptTemplateController {

    private final PromptManagementService promptManagementService;
    private final EntityDtoMapper mapper;

    @Operation(summary = "Create a new prompt template")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Template created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Template already exists",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    @PreAuthorize("hasAuthority('TEMPLATE_CREATE')")
    public ResponseEntity<PromptTemplateResponse> createTemplate(
            @Valid @RequestBody PromptTemplateRequest request) {
        log.debug("REST request to create prompt template: {}", request.getName());
        PromptTemplate template = promptManagementService.createTemplate(request);
        PromptTemplateResponse response = mapper.toTemplateResponse(template);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get a prompt template by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Template found"),
            @ApiResponse(responseCode = "404", description = "Template not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('TEMPLATE_READ')")
    public ResponseEntity<PromptTemplateResponse> getTemplateById(
            @Parameter(description = "Template ID") @PathVariable String id,
            @Parameter(description = "Include versions in response") @RequestParam(required = false, defaultValue = "false") boolean includeVersions) {
        log.debug("REST request to get prompt template: {}", id);

        Optional<PromptTemplate> templateOpt = includeVersions ?
                promptManagementService.getTemplateWithVersions(id) :
                promptManagementService.getTemplateById(id);

        return templateOpt.map(template -> {
            PromptTemplateResponse response = includeVersions ?
                    mapper.toTemplateResponseWithVersions(template) :
                    mapper.toTemplateResponse(template);
            return ResponseEntity.ok(response);
        }).orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Update a prompt template")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Template updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Template not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Template name already exists",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('TEMPLATE_UPDATE')")
    public ResponseEntity<PromptTemplateResponse> updateTemplate(
            @Parameter(description = "Template ID") @PathVariable String id,
            @Valid @RequestBody PromptTemplateRequest request) {
        log.debug("REST request to update prompt template: {}", id);
        PromptTemplate template = promptManagementService.updateTemplate(id, request);
        PromptTemplateResponse response = mapper.toTemplateResponse(template);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Delete a prompt template")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Template deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Template not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('TEMPLATE_DELETE')")
    public ResponseEntity<Void> deleteTemplate(
            @Parameter(description = "Template ID") @PathVariable String id) {
        log.debug("REST request to delete prompt template: {}", id);
        promptManagementService.deleteTemplate(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get templates by project ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Templates found")
    })
    @GetMapping("/by-project/{projectId}")
    @PreAuthorize("hasAuthority('TEMPLATE_READ')")
    public ResponseEntity<PageResponse<PromptTemplateResponse>> getTemplatesByProject(
            @Parameter(description = "Project ID") @PathVariable String projectId,
            @PageableDefault(size = 20, sort = "updatedAt") Pageable pageable) {
        log.debug("REST request to get templates by project: {}", projectId);
        Page<PromptTemplate> templatesPage = promptManagementService.getTemplatesByProject(projectId, pageable);
        PageResponse<PromptTemplateResponse> response = mapper.toPageResponse(
                templatesPage, mapper::toTemplateResponse);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get templates by category")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Templates found")
    })
    @GetMapping("/by-category/{category}")
    @PreAuthorize("hasAuthority('TEMPLATE_READ')")
    public ResponseEntity<PageResponse<PromptTemplateResponse>> getTemplatesByCategory(
            @Parameter(description = "Category name") @PathVariable String category,
            @PageableDefault(size = 20, sort = "updatedAt") Pageable pageable) {
        log.debug("REST request to get templates by category: {}", category);
        Page<PromptTemplate> templatesPage = promptManagementService.getTemplatesByCategory(category, pageable);
        PageResponse<PromptTemplateResponse> response = mapper.toPageResponse(
                templatesPage, mapper::toTemplateResponse);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Search prompt templates with criteria")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Search results")
    })
    @GetMapping("/search")
    @PreAuthorize("hasAuthority('TEMPLATE_READ')")
    public ResponseEntity<PageResponse<PromptTemplateResponse>> searchTemplates(
            @Parameter(description = "Search text") @RequestParam(required = false) String searchText,
            @Parameter(description = "Project ID") @RequestParam(required = false) String projectId,
            @Parameter(description = "Category") @RequestParam(required = false) String category,
            @Parameter(description = "Created by") @RequestParam(required = false) String createdBy,
            @Parameter(description = "Has published version") @RequestParam(required = false) Boolean hasPublishedVersion,
            @Parameter(description = "Minimum version count") @RequestParam(required = false) Integer minVersionCount,
            @Parameter(description = "Use exact match for text search") @RequestParam(required = false) Boolean useExactMatch,
            @Parameter(description = "Use fuzzy match for text search") @RequestParam(required = false) Boolean useFuzzyMatch,
            @PageableDefault(size = 20, sort = "updatedAt") Pageable pageable) {

        log.debug("REST request to search prompt templates");

        PromptTemplateSearchCriteria criteria = PromptTemplateSearchCriteria.builder()
                .searchText(searchText)
                .projectId(projectId)
                .category(category)
                .createdBy(createdBy)
                .hasPublishedVersion(hasPublishedVersion)
                .minVersionCount(minVersionCount)
                .useExactMatch(useExactMatch)
                .useFuzzyMatch(useFuzzyMatch)
                .build();

        Page<PromptTemplate> templatesPage = promptManagementService.searchTemplates(criteria, pageable);
        PageResponse<PromptTemplateResponse> response = mapper.toPageResponse(
                templatesPage, mapper::toTemplateResponse);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get all available categories")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Categories found")
    })
    @GetMapping("/categories")
    @PreAuthorize("hasAuthority('TEMPLATE_READ')")
    public ResponseEntity<List<String>> getAllCategories() {
        log.debug("REST request to get all prompt template categories");
        List<String> categories = promptManagementService.getAllCategories();
        return ResponseEntity.ok(categories);
    }
}