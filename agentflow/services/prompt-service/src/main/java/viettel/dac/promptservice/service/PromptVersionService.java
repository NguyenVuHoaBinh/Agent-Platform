package viettel.dac.promptservice.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import viettel.dac.promptservice.dto.request.PromptVersionRequest;
import viettel.dac.promptservice.dto.response.VersionComparisonResult;
import viettel.dac.promptservice.model.entity.PromptVersion;
import viettel.dac.promptservice.model.entity.VersionAuditEntry;
import viettel.dac.promptservice.model.enums.VersionStatus;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing prompt versions with semantic versioning capabilities.
 */
public interface PromptVersionService {
    /**
     * Create a new version for a template.
     *
     * @param request Version creation request
     * @return The created version
     */
    PromptVersion createVersion(PromptVersionRequest request);

    /**
     * Get a version by ID.
     *
     * @param id The version ID
     * @return Optional containing the version if found
     */
    Optional<PromptVersion> getVersionById(String id);

    /**
     * Get a version by ID with parameters loaded.
     *
     * @param id The version ID
     * @return Optional containing the version with parameters if found
     */
    Optional<PromptVersion> getVersionWithParameters(String id);

    /**
     * Get versions for a template with pagination.
     *
     * @param templateId The template ID
     * @param pageable Pagination parameters
     * @return Page of versions for the template
     */
    Page<PromptVersion> getVersionsByTemplate(String templateId, Pageable pageable);

    /**
     * Get all versions for a template.
     *
     * @param templateId The template ID
     * @return List of all versions for the template
     */
    List<PromptVersion> getAllVersionsByTemplate(String templateId);

    /**
     * Create a new branch from an existing version.
     *
     * @param sourceVersionId The source version ID
     * @param request Version creation request with branch information
     * @return The newly created branch version
     */
    PromptVersion createBranch(String sourceVersionId, PromptVersionRequest request);

    /**
     * Get the version lineage (parent chain) for a version.
     *
     * @param versionId The version ID
     * @return List of versions in the ancestry chain
     */
    List<PromptVersion> getVersionLineage(String versionId);

    /**
     * Update the status of a version.
     *
     * @param versionId The version ID
     * @param newStatus The new status
     * @return The updated version
     */
    PromptVersion updateVersionStatus(String versionId, VersionStatus newStatus);

    /**
     * Check if a version can transition to the specified status.
     *
     * @param versionId The version ID
     * @param newStatus The target status
     * @return True if transition is allowed
     */
    boolean canTransitionToStatus(String versionId, VersionStatus newStatus);

    /**
     * Validate a version number format.
     *
     * @param versionNumber The version number to validate
     * @return True if the version number is valid
     */
    boolean isValidVersionNumber(String versionNumber);

    /**
     * Parse a version number into its components.
     *
     * @param versionNumber The version number to parse
     * @return Array of integers [major, minor, patch]
     */
    int[] parseVersionNumber(String versionNumber);

    /**
     * Get the complete version history for a template, in chronological order.
     *
     * @param templateId The template ID
     * @return List of versions in chronological order
     */
    List<PromptVersion> getVersionHistory(String templateId);

    /**
     * Compare two versions and generate a detailed diff.
     *
     * @param versionId1 First version ID
     * @param versionId2 Second version ID
     * @return VersionComparisonResult containing content and parameter differences
     */
    VersionComparisonResult compareVersions(String versionId1, String versionId2);

    /**
     * Rollback to a previous version.
     *
     * @param versionId The version ID to rollback to
     * @param preserveHistory If true, creates a new version; if false, reverts the current version
     * @return The new or updated version after rollback
     */
    PromptVersion rollbackToVersion(String versionId, boolean preserveHistory);

    /**
     * Get version audit trail including actions like status changes and rollbacks.
     *
     * @param versionId The version ID
     * @return List of audit entries for the version
     */
    List<VersionAuditEntry> getVersionAuditTrail(String versionId);


}