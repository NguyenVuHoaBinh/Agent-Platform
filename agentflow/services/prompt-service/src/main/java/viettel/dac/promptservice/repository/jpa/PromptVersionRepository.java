package viettel.dac.promptservice.repository.jpa;

import org.springframework.cache.annotation.CacheEvict;
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
 * Repository for prompt versions
 */
@Repository
public interface PromptVersionRepository extends JpaRepository<PromptVersion, String> {

    @Override
    @Cacheable(value = "promptVersions", key = "#id")
    Optional<PromptVersion> findById(String id);

    @Override
    @CacheEvict(value = "promptVersions", key = "#entity.id")
    <S extends PromptVersion> S save(S entity);

    @Override
    @CacheEvict(value = "promptVersions", key = "#id")
    void deleteById(String id);

    @Query("SELECT v FROM PromptVersion v LEFT JOIN FETCH v.parameters WHERE v.id = :id")
    @Cacheable(value = "promptVersions", key = "'withParams_' + #id")
    Optional<PromptVersion> findByIdWithParameters(@Param("id") String id);

    Page<PromptVersion> findByTemplateId(String templateId, Pageable pageable);

    List<PromptVersion> findByTemplateId(String templateId);

    Page<PromptVersion> findByTemplateIdAndStatus(String templateId, VersionStatus status, Pageable pageable);

    Optional<PromptVersion> findByTemplateIdAndVersionNumber(String templateId, String versionNumber);

    @Query("SELECT v FROM PromptVersion v WHERE v.template.id = :templateId AND v.status = 'PUBLISHED' " +
            "ORDER BY v.createdAt DESC")
    List<PromptVersion> findPublishedVersionsByTemplateId(@Param("templateId") String templateId, Pageable pageable);

    @Query("SELECT v FROM PromptVersion v WHERE v.template.id = :templateId AND v.status = 'PUBLISHED' " +
            "ORDER BY v.createdAt DESC")
    Optional<PromptVersion> findLatestPublishedVersion(@Param("templateId") String templateId, Pageable pageable);
}
