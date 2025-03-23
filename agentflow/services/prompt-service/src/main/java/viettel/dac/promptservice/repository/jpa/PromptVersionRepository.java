package viettel.dac.promptservice.repository.jpa;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import viettel.dac.promptservice.model.entity.PromptVersion;
import viettel.dac.promptservice.model.enums.VersionStatus;

import java.util.List;
import java.util.Optional;

/**
 * Enhanced repository for prompt versions
 */
@Repository
public interface PromptVersionRepository extends JpaRepository<PromptVersion, String> {

    @Override
    @Cacheable(value = "promptVersions", key = "#id")
    Optional<PromptVersion> findById(String id);

    @Override
    @CachePut(value = "promptVersions", key = "#entity.id")
    <S extends PromptVersion> S save(S entity);

    @Override
    @CacheEvict(value = "promptVersions", key = "#id")
    void deleteById(String id);

    /**
     * Find version by ID with parameters loaded
     */
    @Query("SELECT v FROM PromptVersion v LEFT JOIN FETCH v.parameters WHERE v.id = :id")
    @Cacheable(value = "promptVersions", key = "'withParams_' + #id")
    Optional<PromptVersion> findByIdWithParameters(@Param("id") String id);

    /**
     * Find versions by template ID
     */
    @Cacheable(value = "promptVersionsByTemplate", key = "#templateId + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    Page<PromptVersion> findByTemplateId(String templateId, Pageable pageable);

    /**
     * Get all versions by template ID (non-paginated)
     */
    @Cacheable(value = "promptVersionsByTemplate", key = "'list_' + #templateId")
    List<PromptVersion> findByTemplateId(String templateId);

    /**
     * Find versions by template ID and status
     */
    @Cacheable(value = "promptVersionsByTemplateAndStatus", key = "#templateId + '_' + #status + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    Page<PromptVersion> findByTemplateIdAndStatus(String templateId, VersionStatus status, Pageable pageable);

    /**
     * Find version by template ID and version number
     */
    @Cacheable(value = "promptVersionsByTemplateAndNumber", key = "#templateId + '_' + #versionNumber")
    Optional<PromptVersion> findByTemplateIdAndVersionNumber(String templateId, String versionNumber);

    /**
     * Find published versions by template ID
     */
    @Query("SELECT v FROM PromptVersion v WHERE v.template.id = :templateId AND v.status = 'PUBLISHED' " +
            "ORDER BY v.createdAt DESC")
    @Cacheable(value = "publishedVersionsByTemplate", key = "#templateId + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    List<PromptVersion> findPublishedVersionsByTemplateId(@Param("templateId") String templateId, Pageable pageable);

    /**
     * Find latest published version for a template
     */
    @Query("SELECT v FROM PromptVersion v WHERE v.template.id = :templateId AND v.status = 'PUBLISHED' " +
            "ORDER BY v.createdAt DESC")
    @Cacheable(value = "latestPublishedVersion", key = "#templateId")
    Optional<PromptVersion> findLatestPublishedVersion(@Param("templateId") String templateId);

    /**
     * Find versions by parent version ID
     */
    @Query("SELECT v FROM PromptVersion v WHERE v.parentVersion.id = :parentVersionId")
    List<PromptVersion> findByParentVersionId(@Param("parentVersionId") String parentVersionId);

    /**
     * Find versions by creator
     */
    Page<PromptVersion> findByCreatedBy(String createdBy, Pageable pageable);

    /**
     * Find versions with specific content pattern
     */
    @Query("SELECT v FROM PromptVersion v WHERE v.content LIKE %:pattern%")
    List<PromptVersion> findByContentPattern(@Param("pattern") String pattern);

    /**
     * Count versions by template ID
     */
    long countByTemplateId(String templateId);

    /**
     * Count versions by template ID and status
     */
    long countByTemplateIdAndStatus(String templateId, VersionStatus status);

    /**
     * Get version with the lowest version number for a template
     */
    @Query("SELECT v FROM PromptVersion v WHERE v.template.id = :templateId ORDER BY " +
            "CAST(SUBSTRING(v.versionNumber, 1, LOCATE('.', v.versionNumber) - 1) AS int), " +
            "CAST(SUBSTRING(v.versionNumber, LOCATE('.', v.versionNumber) + 1, LOCATE('.', v.versionNumber, LOCATE('.', v.versionNumber) + 1) - LOCATE('.', v.versionNumber) - 1) AS int), " +
            "CAST(SUBSTRING(v.versionNumber, LOCATE('.', v.versionNumber, LOCATE('.', v.versionNumber) + 1) + 1) AS int)")
    List<PromptVersion> findFirstVersionForTemplate(@Param("templateId") String templateId, Pageable pageable);

    /**
     * Get version history (all versions in lineage)
     */
    @Query(value = "WITH RECURSIVE version_lineage(id, parent_id, depth) AS (" +
            "SELECT v.id, v.parent_version_id, 0 FROM prompt_version v WHERE v.id = :versionId " +
            "UNION ALL " +
            "SELECT v.id, v.parent_version_id, vl.depth + 1 " +
            "FROM prompt_version v JOIN version_lineage vl ON v.id = vl.parent_id " +
            "WHERE v.parent_version_id IS NOT NULL) " +
            "SELECT * FROM prompt_version v WHERE v.id IN (SELECT id FROM version_lineage) " +
            "ORDER BY v.created_at DESC",
            nativeQuery = true)
    List<PromptVersion> findVersionLineage(@Param("versionId") String versionId);

}