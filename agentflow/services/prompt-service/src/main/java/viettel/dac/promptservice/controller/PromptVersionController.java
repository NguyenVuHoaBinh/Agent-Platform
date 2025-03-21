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
import viettel.dac.promptservice.dto.request.PromptVersionRequest;
import viettel.dac.promptservice.dto.response.*;
import viettel.dac.promptservice.model.entity.PromptVersion;
import viettel.dac.promptservice.model.entity.VersionAuditEntry;
import viettel.dac.promptservice.model.enums.VersionStatus;
import viettel.dac.promptservice.service.PromptVersionService;
import viettel.dac.promptservice.service.mapper.EntityDtoMapper;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/versions")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Prompt Versions", description = "API endpoints for managing prompt versions")
public class PromptVersionController {

    private final PromptVersionService versionService;
    private final EntityDtoMapper mapper;

    @Operation(summary = "Create a new prompt version")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Version created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Template not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    @PreAuthorize("hasAuthority('VERSION_CREATE')")
    public ResponseEntity<PromptVersionResponse> createVersion(
            @Valid @RequestBody PromptVersionRequest request) {
        log.debug("REST request to create prompt version for template: {}", request.getTemplateId());
        PromptVersion version = versionService.createVersion(request);
        PromptVersionResponse response = mapper.toVersionResponse(version);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get a prompt version by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Version found"),
            @ApiResponse(responseCode = "404", description = "Version not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('VERSION_READ')")
    public ResponseEntity<PromptVersionResponse> getVersionById(
            @Parameter(description = "Version ID") @PathVariable String id) {
        log.debug("REST request to get prompt version: {}", id);

        Optional<PromptVersion> versionOpt = versionService.getVersionWithParameters(id);

        return versionOpt.map(version -> {
            PromptVersionResponse response = mapper.toVersionResponse(version);
            return ResponseEntity.ok(response);
        }).orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get versions by template ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Versions found"),
            @ApiResponse(responseCode = "404", description = "Template not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/by-template/{templateId}")
    @PreAuthorize("hasAuthority('VERSION_READ')")
    public ResponseEntity<PageResponse<PromptVersionResponse>> getVersionsByTemplate(
            @Parameter(description = "Template ID") @PathVariable String templateId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        log.debug("REST request to get versions by template: {}", templateId);
        Page<PromptVersion> versionsPage = versionService.getVersionsByTemplate(templateId, pageable);
        PageResponse<PromptVersionResponse> response = mapper.toPageResponse(
                versionsPage, mapper::toVersionResponse);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update version status")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Status updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid status transition",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Version not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('VERSION_UPDATE')")
    public ResponseEntity<PromptVersionResponse> updateVersionStatus(
            @Parameter(description = "Version ID") @PathVariable String id,
            @Parameter(description = "New status") @RequestParam VersionStatus status) {
        log.debug("REST request to update version status: {} to {}", id, status);

        // Check if transition is allowed
        if (!versionService.canTransitionToStatus(id, status)) {
            return ResponseEntity.badRequest().build();
        }

        PromptVersion version = versionService.updateVersionStatus(id, status);
        PromptVersionResponse response = mapper.toVersionResponse(version);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Create a branch from existing version")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Branch created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Source version not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{sourceVersionId}/branch")
    @PreAuthorize("hasAuthority('VERSION_CREATE')")
    public ResponseEntity<PromptVersionResponse> createBranch(
            @Parameter(description = "Source version ID") @PathVariable String sourceVersionId,
            @Valid @RequestBody PromptVersionRequest request) {
        log.debug("REST request to create branch from version: {}", sourceVersionId);
        PromptVersion branch = versionService.createBranch(sourceVersionId, request);
        PromptVersionResponse response = mapper.toVersionResponse(branch);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Compare two versions")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Comparison result"),
            @ApiResponse(responseCode = "400", description = "Invalid request",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Version not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/compare")
    @PreAuthorize("hasAuthority('VERSION_READ')")
    public ResponseEntity<VersionComparisonResult> compareVersions(
            @Parameter(description = "First version ID") @RequestParam String version1Id,
            @Parameter(description = "Second version ID") @RequestParam String version2Id) {
        log.debug("REST request to compare versions: {} and {}", version1Id, version2Id);
        VersionComparisonResult result = versionService.compareVersions(version1Id, version2Id);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Rollback to a previous version")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rollback successful"),
            @ApiResponse(responseCode = "400", description = "Invalid request",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Version not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{versionId}/rollback")
    @PreAuthorize("hasAuthority('VERSION_UPDATE')")
    public ResponseEntity<PromptVersionResponse> rollbackToVersion(
            @Parameter(description = "Target version ID") @PathVariable String versionId,
            @Parameter(description = "Preserve history") @RequestParam(defaultValue = "true") boolean preserveHistory) {
        log.debug("REST request to rollback to version: {}, preserveHistory: {}", versionId, preserveHistory);
        PromptVersion version = versionService.rollbackToVersion(versionId, preserveHistory);
        PromptVersionResponse response = mapper.toVersionResponse(version);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get version lineage (parent chain)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lineage found"),
            @ApiResponse(responseCode = "404", description = "Version not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}/lineage")
    @PreAuthorize("hasAuthority('VERSION_READ')")
    public ResponseEntity<List<PromptVersionResponse>> getVersionLineage(
            @Parameter(description = "Version ID") @PathVariable String id) {
        log.debug("REST request to get version lineage: {}", id);
        List<PromptVersion> lineage = versionService.getVersionLineage(id);
        List<PromptVersionResponse> response = lineage.stream()
                .map(mapper::toVersionResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get version audit trail")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Audit trail found"),
            @ApiResponse(responseCode = "404", description = "Version not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}/audit-trail")
    @PreAuthorize("hasAuthority('VERSION_READ')")
    public ResponseEntity<List<VersionAuditEntry>> getVersionAuditTrail(
            @Parameter(description = "Version ID") @PathVariable String id) {
        log.debug("REST request to get version audit trail: {}", id);
        List<VersionAuditEntry> auditTrail = versionService.getVersionAuditTrail(id);
        return ResponseEntity.ok(auditTrail);
    }

    @Operation(summary = "Get template version history")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "History found"),
            @ApiResponse(responseCode = "404", description = "Template not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/history/{templateId}")
    @PreAuthorize("hasAuthority('VERSION_READ')")
    public ResponseEntity<List<PromptVersionResponse>> getVersionHistory(
            @Parameter(description = "Template ID") @PathVariable String templateId) {
        log.debug("REST request to get version history for template: {}", templateId);
        List<PromptVersion> history = versionService.getVersionHistory(templateId);
        List<PromptVersionResponse> response = history.stream()
                .map(mapper::toVersionResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }
}