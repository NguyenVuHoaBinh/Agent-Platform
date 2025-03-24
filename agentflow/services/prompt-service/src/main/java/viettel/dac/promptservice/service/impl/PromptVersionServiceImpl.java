package viettel.dac.promptservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import viettel.dac.promptservice.dto.request.PromptParameterRequest;
import viettel.dac.promptservice.dto.request.PromptVersionRequest;
import viettel.dac.promptservice.dto.response.VersionComparisonResult;
import viettel.dac.promptservice.dto.response.VersionComparisonResult.ParameterDiff;
import viettel.dac.promptservice.exception.ResourceNotFoundException;
import viettel.dac.promptservice.exception.ValidationException;
import viettel.dac.promptservice.model.entity.PromptParameter;
import viettel.dac.promptservice.model.entity.PromptTemplate;
import viettel.dac.promptservice.model.entity.PromptVersion;
import viettel.dac.promptservice.model.entity.VersionAuditEntry;
import viettel.dac.promptservice.model.entity.VersionAuditEntry.AuditActionType;
import viettel.dac.promptservice.model.enums.VersionStatus;
import viettel.dac.promptservice.repository.jpa.PromptParameterRepository;
import viettel.dac.promptservice.repository.jpa.PromptTemplateRepository;
import viettel.dac.promptservice.repository.jpa.PromptVersionRepository;
import viettel.dac.promptservice.repository.jpa.VersionAuditRepository;
import viettel.dac.promptservice.security.SecurityUtils;
import viettel.dac.promptservice.service.PromptVersionService;
import viettel.dac.promptservice.service.event.VersionStatusChangeEvent;
import viettel.dac.promptservice.util.DiffUtility;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PromptVersionServiceImpl implements PromptVersionService {

    private final PromptVersionRepository versionRepository;
    private final PromptTemplateRepository templateRepository;
    private final PromptParameterRepository parameterRepository;
    private final VersionAuditRepository auditRepository;
    private final SecurityUtils securityUtils;
    private final ApplicationEventPublisher eventPublisher;
    private final DiffUtility diffUtility;

    // Regex pattern for semantic versioning (major.minor.patch)
    private static final Pattern VERSION_PATTERN = Pattern.compile("^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)$");

    // Maximum depth for version lineage retrieval to avoid excessive recursion
    private static final int MAX_LINEAGE_DEPTH = 100;

    //------------------------------------------------------------------------------------
    // Basic Version Management
    //------------------------------------------------------------------------------------

    @Override
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    @CachePut(value = "promptVersions", key = "#result.id")
    public PromptVersion createVersion(PromptVersionRequest request) {
        log.debug("Creating new prompt version for template: {}", request.getTemplateId());

        // Validate request and version number
        validateVersionRequest(request);

        // Get template by ID
        PromptTemplate template = templateRepository.findById(request.getTemplateId())
                .orElseThrow(() -> new ResourceNotFoundException("Template not found with id: " + request.getTemplateId()));

        // Get parent version if specified
        PromptVersion parentVersion = null;
        if (request.getParentVersionId() != null) {
            parentVersion = versionRepository.findById(request.getParentVersionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent version not found with id: " + request.getParentVersionId()));

            // Ensure parent version belongs to the same template
            if (!parentVersion.getTemplate().getId().equals(template.getId())) {
                throw new ValidationException("Parent version must belong to the same template");
            }
        }

        // Get current user
        String currentUserId = securityUtils.getCurrentUserId()
                .orElse("system");

        // Check if version number already exists for this template
        if (versionRepository.findByTemplateIdAndVersionNumber(template.getId(), request.getVersionNumber()).isPresent()) {
            throw new ValidationException("Version number already exists for this template: " + request.getVersionNumber());
        }


        // Create version entity
        PromptVersion version = PromptVersion.builder()
                .template(template)
                .versionNumber(request.getVersionNumber())
                .content(request.getContent())
                .status(VersionStatus.DRAFT) // Always start as draft
                .createdBy(currentUserId)
                .parentVersion(parentVersion)
                .build();

        // Save version
        PromptVersion savedVersion = versionRepository.save(version);

        // Process and add parameters
        if (request.getParameters() != null) {
            for (PromptParameterRequest paramRequest : request.getParameters()) {
                PromptParameter parameter = PromptParameter.builder()
                        .version(savedVersion)
                        .name(paramRequest.getName())
                        .description(paramRequest.getDescription())
                        .parameterType(paramRequest.getParameterType())
                        .defaultValue(paramRequest.getDefaultValue())
                        .required(paramRequest.isRequired())
                        .validationPattern(paramRequest.getValidationPattern())
                        .build();

                savedVersion.addParameter(parameter);
            }

            // Save again to persist parameters
            savedVersion = versionRepository.save(savedVersion);
        }

        // Create audit entry for version creation
        VersionAuditEntry auditEntry = VersionAuditEntry.builder()
                .version(savedVersion)
                .actionType(AuditActionType.CREATED)
                .performedBy(currentUserId)
                .performedAt(LocalDateTime.now())
                .details("Version created" + (parentVersion != null ? " from parent " + parentVersion.getVersionNumber() : ""))
                .newStatus(VersionStatus.DRAFT)
                .build();

        auditRepository.save(auditEntry);

        log.info("Created prompt version with ID: {}, version number: {}",
                savedVersion.getId(), savedVersion.getVersionNumber());
        return savedVersion;
    }

    @Override
    @Cacheable(value = "promptVersions", key = "#id", unless = "#result == null")
    public Optional<PromptVersion> getVersionById(String id) {
        log.debug("Fetching prompt version with ID: {}", id);
        return versionRepository.findById(id);
    }

    @Override
    @Cacheable(value = "promptVersionsWithParams", key = "#id", unless = "#result == null")
    public Optional<PromptVersion> getVersionWithParameters(String id) {
        log.debug("Fetching prompt version with parameters, ID: {}", id);
        return versionRepository.findByIdWithParameters(id);
    }

    @Override
    @Cacheable(value = "promptVersionsByTemplate",
            key = "#templateId + '_' + #pageable.pageNumber + '_' + #pageable.pageSize",
            unless = "#result.totalElements == 0")
    public Page<PromptVersion> getVersionsByTemplate(String templateId, Pageable pageable) {
        log.debug("Fetching versions for template: {}", templateId);

        // Verify template exists
        if (!templateRepository.existsById(templateId)) {
            throw new ResourceNotFoundException("Template not found with id: " + templateId);
        }

        return versionRepository.findByTemplateId(templateId, pageable);
    }

    @Override
    @Cacheable(value = "allPromptVersionsByTemplate", key = "#templateId", unless = "#result.empty")
    public List<PromptVersion> getAllVersionsByTemplate(String templateId) {
        log.debug("Fetching all versions for template: {}", templateId);

        // Verify template exists
        if (!templateRepository.existsById(templateId)) {
            throw new ResourceNotFoundException("Template not found with id: " + templateId);
        }

        return versionRepository.findByTemplateId(templateId, PageRequest.of(0, Integer.MAX_VALUE)).getContent();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    @Caching(
            put = {
                    @CachePut(value = "promptVersions", key = "#result.id")
            },
            evict = {
                    @CacheEvict(value = "promptVersionsByTemplate",
                            key = "#result.template.id + '_*'"),
                    @CacheEvict(value = "allPromptVersionsByTemplate",
                            key = "#result.template.id"),
                    @CacheEvict(value = "promptVersionsWithParams",
                            key = "#versionId")
            }
    )
    public PromptVersion updateVersionStatus(String versionId, VersionStatus newStatus) {
        log.debug("Updating status of version {} to {}", versionId, newStatus);

        // Get version
        PromptVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new ResourceNotFoundException("Version not found with id: " + versionId));

        // Check if status transition is allowed
        if (!version.canTransitionTo(newStatus)) {
            throw new ValidationException(
                    "Cannot transition from " + version.getStatus() + " to " + newStatus);
        }

        // Store old status for event
        VersionStatus oldStatus = version.getStatus();

        // Special handling for PUBLISHED status
        if (newStatus == VersionStatus.PUBLISHED) {
            // Check if there's already a published version for this template
            List<PromptVersion> publishedVersions = versionRepository.findByTemplateIdAndStatus(
                            version.getTemplate().getId(), VersionStatus.PUBLISHED, PageRequest.of(0, Integer.MAX_VALUE))
                    .getContent();

            // Deprecate current published versions
            for (PromptVersion publishedVersion : publishedVersions) {
                publishedVersion.setStatus(VersionStatus.DEPRECATED);
                versionRepository.save(publishedVersion);

                // Create audit entry for this status change
                VersionAuditEntry deprecateAuditEntry = VersionAuditEntry.builder()
                        .version(publishedVersion)
                        .actionType(AuditActionType.STATUS_CHANGED)
                        .performedBy(securityUtils.getCurrentUserId().orElse("system"))
                        .performedAt(LocalDateTime.now())
                        .details("Status changed due to new published version")
                        .previousStatus(VersionStatus.PUBLISHED)
                        .newStatus(VersionStatus.DEPRECATED)
                        .build();

                auditRepository.save(deprecateAuditEntry);

                // Publish event for the status change
                eventPublisher.publishEvent(new VersionStatusChangeEvent(
                        publishedVersion, VersionStatus.PUBLISHED, VersionStatus.DEPRECATED));
            }
        }

        // Update status
        version.setStatus(newStatus);
        PromptVersion updatedVersion = versionRepository.save(version);

        // Create audit entry for the status change
        VersionAuditEntry auditEntry = VersionAuditEntry.builder()
                .version(updatedVersion)
                .actionType(AuditActionType.STATUS_CHANGED)
                .performedBy(securityUtils.getCurrentUserId().orElse("system"))
                .performedAt(LocalDateTime.now())
                .details("Status changed")
                .previousStatus(oldStatus)
                .newStatus(newStatus)
                .build();

        auditRepository.save(auditEntry);

        // Publish event for the status change
        eventPublisher.publishEvent(new VersionStatusChangeEvent(updatedVersion, oldStatus, newStatus));

        log.info("Updated version {} status from {} to {}", versionId, oldStatus, newStatus);
        return updatedVersion;
    }

    //------------------------------------------------------------------------------------
    // Version Branching
    //------------------------------------------------------------------------------------

    @Override
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    @CachePut(value = "promptVersions", key = "#result.id")
    public PromptVersion createBranch(String sourceVersionId, PromptVersionRequest request) {
        log.debug("Creating branch from version: {}", sourceVersionId);

        // Get source version
        PromptVersion sourceVersion = versionRepository.findByIdWithParameters(sourceVersionId)
                .orElseThrow(() -> new ResourceNotFoundException("Source version not found with id: " + sourceVersionId));

        // Validate version number
        if (!isValidVersionNumber(request.getVersionNumber())) {
            throw new ValidationException("Invalid version number format: " + request.getVersionNumber());
        }

        // Check if version number already exists for this template
        if (versionRepository.findByTemplateIdAndVersionNumber(sourceVersion.getTemplate().getId(), request.getVersionNumber()).isPresent()) {
            throw new ValidationException("Version number already exists for this template: " + request.getVersionNumber());
        }


        // Get current user
        String currentUserId = securityUtils.getCurrentUserId()
                .orElse("system");

        // Determine content - use source version's content if not provided
        String content = request.getContent() != null ? request.getContent() : sourceVersion.getContent();

        // Create branch version
        PromptVersion branchVersion = PromptVersion.builder()
                .template(sourceVersion.getTemplate())
                .versionNumber(request.getVersionNumber())
                .content(content)
                .status(VersionStatus.DRAFT)
                .createdBy(currentUserId)
                .parentVersion(sourceVersion)
                .build();

        // Save branch version
        PromptVersion savedBranch = versionRepository.save(branchVersion);

        // Copy parameters from source version if none provided
        if ((request.getParameters() == null || request.getParameters().isEmpty()) &&
                !sourceVersion.getParameters().isEmpty()) {

            // Clone parameters from source version
            for (PromptParameter sourceParam : sourceVersion.getParameters()) {
                PromptParameter newParam = PromptParameter.builder()
                        .version(savedBranch)
                        .name(sourceParam.getName())
                        .description(sourceParam.getDescription())
                        .parameterType(sourceParam.getParameterType())
                        .defaultValue(sourceParam.getDefaultValue())
                        .required(sourceParam.isRequired())
                        .validationPattern(sourceParam.getValidationPattern())
                        .build();

                savedBranch.addParameter(newParam);
            }
        } else if (request.getParameters() != null) {
            // Use parameters from request
            for (PromptParameterRequest paramRequest : request.getParameters()) {
                PromptParameter parameter = PromptParameter.builder()
                        .version(savedBranch)
                        .name(paramRequest.getName())
                        .description(paramRequest.getDescription())
                        .parameterType(paramRequest.getParameterType())
                        .defaultValue(paramRequest.getDefaultValue())
                        .required(paramRequest.isRequired())
                        .validationPattern(paramRequest.getValidationPattern())
                        .build();

                savedBranch.addParameter(parameter);
            }
        }

        // Save again to persist parameters
        savedBranch = versionRepository.save(savedBranch);

        // Create audit entry for branch creation
        VersionAuditEntry auditEntry = VersionAuditEntry.builder()
                .version(savedBranch)
                .actionType(AuditActionType.BRANCHED)
                .performedBy(currentUserId)
                .performedAt(LocalDateTime.now())
                .details("Created as branch from version " + sourceVersion.getVersionNumber())
                .newStatus(VersionStatus.DRAFT)
                .referenceVersionId(sourceVersion.getId())
                .build();

        auditRepository.save(auditEntry);

        log.info("Created branch version with ID: {}, version number: {} from source version: {}",
                savedBranch.getId(), savedBranch.getVersionNumber(), sourceVersionId);
        return savedBranch;
    }

    @Override
    @Cacheable(value = "versionLineage", key = "#versionId", unless = "#result.empty")
    public List<PromptVersion> getVersionLineage(String versionId) {
        log.debug("Getting lineage for version: {}", versionId);

        // Verify version exists
        if (!versionRepository.existsById(versionId)) {
            throw new ResourceNotFoundException("Version not found with id: " + versionId);
        }

        // Use repository method to get version lineage
        return versionRepository.findVersionLineage(versionId);
    }

    //------------------------------------------------------------------------------------
    // Version History & Comparison
    //------------------------------------------------------------------------------------

    @Override
    @Cacheable(value = "versionHistory", key = "#templateId", unless = "#result.isEmpty()")
    public List<PromptVersion> getVersionHistory(String templateId) {
        log.debug("Getting version history for template: {}", templateId);

        // Verify template exists
        if (!templateRepository.existsById(templateId)) {
            throw new ResourceNotFoundException("Template not found with id: " + templateId);
        }

        // Get all versions ordered by creation date
        return versionRepository.findByTemplateId(templateId,
                        PageRequest.of(0, Integer.MAX_VALUE, Sort.by(Sort.Direction.ASC, "createdAt")))
                .getContent();
    }

    @Override
    public VersionComparisonResult compareVersions(String versionId1, String versionId2) {
        log.debug("Comparing versions: {} and {}", versionId1, versionId2);

        // Get versions with parameters
        PromptVersion version1 = versionRepository.findByIdWithParameters(versionId1)
                .orElseThrow(() -> new ResourceNotFoundException("Version not found with id: " + versionId1));

        PromptVersion version2 = versionRepository.findByIdWithParameters(versionId2)
                .orElseThrow(() -> new ResourceNotFoundException("Version not found with id: " + versionId2));

        // Check if versions belong to the same template
        if (!version1.getTemplate().getId().equals(version2.getTemplate().getId())) {
            throw new ValidationException("Cannot compare versions from different templates");
        }

        // Generate content diff
        List<VersionComparisonResult.TextDiff> contentDiffs =
                diffUtility.consolidateDiffs(diffUtility.generateTextDiff(version1.getContent(), version2.getContent()));

        // Compare parameters
        Set<String> paramNames1 = version1.getParameters().stream()
                .map(PromptParameter::getName)
                .collect(Collectors.toSet());

        Set<String> paramNames2 = version2.getParameters().stream()
                .map(PromptParameter::getName)
                .collect(Collectors.toSet());

        // Find added, removed, and common parameters
        Set<String> addedParamNames = new HashSet<>(paramNames2);
        addedParamNames.removeAll(paramNames1);

        Set<String> removedParamNames = new HashSet<>(paramNames1);
        removedParamNames.removeAll(paramNames2);

        Set<String> commonParamNames = new HashSet<>(paramNames1);
        commonParamNames.retainAll(paramNames2);

        // Build parameter lists for the result
        List<PromptParameter> addedParameters = version2.getParameters().stream()
                .filter(p -> addedParamNames.contains(p.getName()))
                .collect(Collectors.toList());

        List<PromptParameter> removedParameters = version1.getParameters().stream()
                .filter(p -> removedParamNames.contains(p.getName()))
                .collect(Collectors.toList());

        // Compare common parameters for changes
        Map<String, PromptParameter> paramMap1 = version1.getParameters().stream()
                .collect(Collectors.toMap(PromptParameter::getName, p -> p));

        Map<String, PromptParameter> paramMap2 = version2.getParameters().stream()
                .collect(Collectors.toMap(PromptParameter::getName, p -> p));

        List<ParameterDiff> modifiedParameters = new ArrayList<>();

        for (String paramName : commonParamNames) {
            PromptParameter param1 = paramMap1.get(paramName);
            PromptParameter param2 = paramMap2.get(paramName);

            if (!Objects.equals(param1.getDescription(), param2.getDescription()) ||
                    param1.getParameterType() != param2.getParameterType() ||
                    param1.isRequired() != param2.isRequired() ||
                    !Objects.equals(param1.getDefaultValue(), param2.getDefaultValue()) ||
                    !Objects.equals(param1.getValidationPattern(), param2.getValidationPattern())) {

                // Parameter has been modified
                Map<String, Object> originalValues = new HashMap<>();
                originalValues.put("description", param1.getDescription());
                originalValues.put("parameterType", param1.getParameterType());
                originalValues.put("required", param1.isRequired());
                originalValues.put("defaultValue", param1.getDefaultValue());
                originalValues.put("validationPattern", param1.getValidationPattern());

                Map<String, Object> newValues = new HashMap<>();
                newValues.put("description", param2.getDescription());
                newValues.put("parameterType", param2.getParameterType());
                newValues.put("required", param2.isRequired());
                newValues.put("defaultValue", param2.getDefaultValue());
                newValues.put("validationPattern", param2.getValidationPattern());

                modifiedParameters.add(ParameterDiff.builder()
                        .parameterName(paramName)
                        .originalValues(originalValues)
                        .newValues(newValues)
                        .build());
            }
        }

        // Build and return the comparison result
        return VersionComparisonResult.builder()
                .versionId1(version1.getId())
                .versionNumber1(version1.getVersionNumber())
                .versionId2(version2.getId())
                .versionNumber2(version2.getVersionNumber())
                .originalContent(version1.getContent())
                .modifiedContent(version2.getContent())
                .contentDiffs(contentDiffs)
                .addedParameters(addedParameters)
                .removedParameters(removedParameters)
                .modifiedParameters(modifiedParameters)
                .build();
    }

    //------------------------------------------------------------------------------------
    // Version Rollback
    //------------------------------------------------------------------------------------

    @Override
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    @Caching(
            put = {
                    @CachePut(value = "promptVersions", key = "#result.id")
            },
            evict = {
                    @CacheEvict(value = "promptVersionsByTemplate", key = "#result.template.id + '_*'"),
                    @CacheEvict(value = "allPromptVersionsByTemplate", key = "#result.template.id"),
                    @CacheEvict(value = "versionHistory", key = "#result.template.id"),
                    @CacheEvict(value = "versionLineage", allEntries = true)
            }
    )
    public PromptVersion rollbackToVersion(String versionId, boolean preserveHistory) {
        log.debug("Rolling back to version: {}, preserveHistory: {}", versionId, preserveHistory);

        // Get source version with parameters
        PromptVersion sourceVersion = versionRepository.findByIdWithParameters(versionId)
                .orElseThrow(() -> new ResourceNotFoundException("Version not found with id: " + versionId));

        // Validate rollback operation
        if (sourceVersion.getStatus() == VersionStatus.ARCHIVED) {
            throw new ValidationException("Cannot rollback to an archived version");
        }

        // Get current user
        String currentUserId = securityUtils.getCurrentUserId()
                .orElse("system");

        PromptVersion resultVersion;

        if (preserveHistory) {
            // Create a new version based on the source version

            // Generate new version number based on current highest version
            String newVersionNumber = generateNextVersionNumber(sourceVersion.getTemplate().getId());

            PromptVersion newVersion = PromptVersion.builder()
                    .template(sourceVersion.getTemplate())
                    .versionNumber(newVersionNumber)
                    .content(sourceVersion.getContent())
                    .status(VersionStatus.DRAFT) // Always start as draft
                    .createdBy(currentUserId)
                    .parentVersion(sourceVersion) // Reference the source as parent
                    .build();

            // Save new version
            resultVersion = versionRepository.save(newVersion);

            // Clone parameters from source version
            for (PromptParameter sourceParam : sourceVersion.getParameters()) {
                PromptParameter newParam = PromptParameter.builder()
                        .version(resultVersion)
                        .name(sourceParam.getName())
                        .description(sourceParam.getDescription())
                        .parameterType(sourceParam.getParameterType())
                        .defaultValue(sourceParam.getDefaultValue())
                        .required(sourceParam.isRequired())
                        .validationPattern(sourceParam.getValidationPattern())
                        .build();

                resultVersion.addParameter(newParam);
            }

            // Save again with parameters
            resultVersion = versionRepository.save(resultVersion);

            // Create rollback audit entry
            VersionAuditEntry auditEntry = VersionAuditEntry.builder()
                    .version(resultVersion)
                    .actionType(AuditActionType.ROLLBACK)
                    .performedBy(currentUserId)
                    .performedAt(LocalDateTime.now())
                    .details("Rollback to version " + sourceVersion.getVersionNumber() + " with history preservation")
                    .referenceVersionId(sourceVersion.getId())
                    .build();

            auditRepository.save(auditEntry);

            log.info("Created new version {} as rollback from version {}",
                    resultVersion.getId(), sourceVersion.getId());

        } else {
            // Get the current active version (latest non-archived version)
            List<PromptVersion> activeVersions = versionRepository.findByTemplateId(
                            sourceVersion.getTemplate().getId(),
                            PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "createdAt")))
                    .getContent();

            if (activeVersions.isEmpty()) {
                throw new ValidationException("No active version found to rollback");
            }

            PromptVersion currentVersion = activeVersions.get(0);

            // Don't rollback to the same version
            if (currentVersion.getId().equals(sourceVersion.getId())) {
                log.info("Rollback unnecessary: target is the current version");
                return currentVersion;
            }

            // Store current state for audit
            String previousContent = currentVersion.getContent();
            VersionStatus previousStatus = currentVersion.getStatus();

            // Update the current version with source version's content
            currentVersion.setContent(sourceVersion.getContent());

            // If source is published, we will maintain published status
            // Otherwise set to draft to require review
            if (sourceVersion.getStatus() == VersionStatus.PUBLISHED) {
                currentVersion.setStatus(VersionStatus.PUBLISHED);
            } else {
                currentVersion.setStatus(VersionStatus.DRAFT);
            }

            // Save updated version
            resultVersion = versionRepository.save(currentVersion);

            // Remove existing parameters
            for (PromptParameter param : new ArrayList<>(currentVersion.getParameters())) {
                currentVersion.removeParameter(param);
                parameterRepository.delete(param);
            }

            // Clone parameters from source version
            for (PromptParameter sourceParam : sourceVersion.getParameters()) {
                PromptParameter newParam = PromptParameter.builder()
                        .version(resultVersion)
                        .name(sourceParam.getName())
                        .description(sourceParam.getDescription())
                        .parameterType(sourceParam.getParameterType())
                        .defaultValue(sourceParam.getDefaultValue())
                        .required(sourceParam.isRequired())
                        .validationPattern(sourceParam.getValidationPattern())
                        .build();

                resultVersion.addParameter(newParam);
            }

            // Save again with updated parameters
            resultVersion = versionRepository.save(resultVersion);

            // Create rollback audit entry
            VersionAuditEntry auditEntry = VersionAuditEntry.builder()
                    .version(resultVersion)
                    .actionType(AuditActionType.ROLLBACK)
                    .performedBy(currentUserId)
                    .performedAt(LocalDateTime.now())
                    .details("Direct rollback to version " + sourceVersion.getVersionNumber())
                    .previousStatus(previousStatus)
                    .newStatus(resultVersion.getStatus())
                    .referenceVersionId(sourceVersion.getId())
                    .build();

            auditRepository.save(auditEntry);

            // If status changed, publish event
            if (previousStatus != resultVersion.getStatus()) {
                eventPublisher.publishEvent(new VersionStatusChangeEvent(
                        resultVersion, previousStatus, resultVersion.getStatus()));
            }

            log.info("Updated version {} by rolling back to version {}",
                    resultVersion.getId(), sourceVersion.getId());
        }

        return resultVersion;
    }

    @Override
    @Cacheable(value = "versionAuditTrail", key = "#versionId", unless = "#result.isEmpty()")
    public List<VersionAuditEntry> getVersionAuditTrail(String versionId) {
        log.debug("Getting audit trail for version: {}", versionId);

        // Verify version exists
        if (!versionRepository.existsById(versionId)) {
            throw new ResourceNotFoundException("Version not found with id: " + versionId);
        }

        return auditRepository.findByVersionIdOrderByPerformedAtDesc(versionId);
    }

    //------------------------------------------------------------------------------------
    // Version Number Management & Validation
    //------------------------------------------------------------------------------------

    @Override
    public boolean canTransitionToStatus(String versionId, VersionStatus newStatus) {
        log.debug("Checking if version {} can transition to {}", versionId, newStatus);

        // Get version
        PromptVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new ResourceNotFoundException("Version not found with id: " + versionId));

        return version.canTransitionTo(newStatus);
    }

    @Override
    public boolean isValidVersionNumber(String versionNumber) {
        return versionNumber != null && VERSION_PATTERN.matcher(versionNumber).matches();
    }

    @Override
    public int[] parseVersionNumber(String versionNumber) {
        if (!isValidVersionNumber(versionNumber)) {
            throw new ValidationException("Invalid version number format: " + versionNumber);
        }

        String[] parts = versionNumber.split("\\.");
        return new int[] {
                Integer.parseInt(parts[0]),
                Integer.parseInt(parts[1]),
                Integer.parseInt(parts[2])
        };
    }

    //------------------------------------------------------------------------------------
    // Helper Methods
    //------------------------------------------------------------------------------------

    /**
     * Generate the next version number based on the highest existing version.
     * Increments the minor version by default.
     *
     * @param templateId The template ID
     * @return The next version number
     */
    private String generateNextVersionNumber(String templateId) {
        // Find the highest version number for this template
        List<PromptVersion> versions = versionRepository.findByTemplateId(
                        templateId,
                        PageRequest.of(0, Integer.MAX_VALUE))
                .getContent();

        if (versions.isEmpty()) {
            return "1.0.0"; // Start with 1.0.0 if no versions exist
        }

        // Parse all version numbers and find the highest
        int highestMajor = 0;
        int highestMinor = 0;
        int highestPatch = 0;

        for (PromptVersion version : versions) {
            if (isValidVersionNumber(version.getVersionNumber())) {
                int[] parts = parseVersionNumber(version.getVersionNumber());

                if (parts[0] > highestMajor) {
                    highestMajor = parts[0];
                    highestMinor = parts[1];
                    highestPatch = parts[2];
                } else if (parts[0] == highestMajor) {
                    if (parts[1] > highestMinor) {
                        highestMinor = parts[1];
                        highestPatch = parts[2];
                    } else if (parts[1] == highestMinor && parts[2] > highestPatch) {
                        highestPatch = parts[2];
                    }
                }
            }
        }

        // Increment minor version
        return String.format("%d.%d.%d", highestMajor, highestMinor + 1, 0);
    }

    /**
     * Validate version request data
     *
     * @param request The version request to validate
     * @throws ValidationException if validation fails
     */
    private void validateVersionRequest(PromptVersionRequest request) {
        Map<String, String> errors = new HashMap<>();

        // Check required fields
        if (request.getTemplateId() == null) {
            errors.put("templateId", "Template ID is required");
        }

        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            errors.put("content", "Content is required");
        }

        // Validate version number format
        if (request.getVersionNumber() != null) {
            if (!isValidVersionNumber(request.getVersionNumber())) {
                errors.put("versionNumber", "Version number must follow format: major.minor.patch (e.g., 1.0.0)");
            }
        } else {
            errors.put("versionNumber", "Version number is required");
        }

        // Validate parameters (unique names, etc.)
        if (request.getParameters() != null && !request.getParameters().isEmpty()) {
            Set<String> paramNames = new HashSet<>();
            for (int i = 0; i < request.getParameters().size(); i++) {
                PromptParameterRequest param = request.getParameters().get(i);

                // Check for duplicate parameter names
                if (param.getName() != null) {
                    if (paramNames.contains(param.getName())) {
                        errors.put("parameters[" + i + "].name", "Duplicate parameter name: " + param.getName());
                    } else {
                        paramNames.add(param.getName());
                    }
                } else {
                    errors.put("parameters[" + i + "].name", "Parameter name is required");
                }

                // Check parameter type
                if (param.getParameterType() == null) {
                    errors.put("parameters[" + i + "].parameterType", "Parameter type is required");
                }
            }
        }

        if (!errors.isEmpty()) {
            throw new ValidationException("Version validation failed", errors);
        }
    }

    /**
     * Creates a PromptVersionRequest from an existing PromptVersion with modified values
     *
     * @param sourceVersion The source version to base the request on
     * @param newVersionNumber The new version number to use
     * @param newContent The new content to use
     * @param comment A comment describing the changes (used for audit)
     * @return A new PromptVersionRequest based on the source version but with updated values
     */
    public PromptVersionRequest createVersionRequestFromExisting(
            PromptVersion sourceVersion, 
            String newVersionNumber, 
            String newContent,
            String comment) {
        
        log.debug("Creating version request from existing version: {}", sourceVersion.getId());
        
        // Convert parameters from entity to request DTO
        List<PromptParameterRequest> parameterRequests = sourceVersion.getParameters().stream()
                .map(param -> PromptParameterRequest.builder()
                        .name(param.getName())
                        .description(param.getDescription())
                        .parameterType(param.getParameterType())
                        .defaultValue(param.getDefaultValue())
                        .required(param.isRequired())
                        .validationPattern(param.getValidationPattern())
                        .build())
                .collect(Collectors.toList());
        
        // Build the request
        return PromptVersionRequest.builder()
                .templateId(sourceVersion.getTemplate().getId())
                .versionNumber(newVersionNumber)
                .content(newContent)
                .systemPrompt(sourceVersion.getSystemPrompt())
                .parentVersionId(sourceVersion.getId()) // Link back to source version
                .parameters(parameterRequests)
                .build();
    }
}