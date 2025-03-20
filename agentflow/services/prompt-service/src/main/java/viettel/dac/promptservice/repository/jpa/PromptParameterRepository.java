package viettel.dac.promptservice.repository.jpa;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import viettel.dac.promptservice.model.entity.PromptParameter;
import viettel.dac.promptservice.model.enums.ParameterType;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Enhanced repository for prompt parameters
 */
@Repository
public interface PromptParameterRepository extends JpaRepository<PromptParameter, String> {

    @Override
    @Cacheable(value = "promptParameters", key = "#id")
    Optional<PromptParameter> findById(String id);

    @Override
    @CachePut(value = "promptParameters", key = "#entity.id")
    <S extends PromptParameter> S save(S entity);

    @Override
    @CacheEvict(value = "promptParameters", key = "#id")
    void deleteById(String id);

    /**
     * Find parameters by version ID
     */
    @Cacheable(value = "parametersByVersion", key = "#versionId")
    List<PromptParameter> findByVersionId(String versionId);

    /**
     * Find parameter by version ID and name
     */
    @Cacheable(value = "parametersByVersionAndName", key = "#versionId + '_' + #name")
    Optional<PromptParameter> findByVersionIdAndName(String versionId, String name);

    /**
     * Find parameters by version ID and parameter type
     */
    @Cacheable(value = "parametersByVersionAndType", key = "#versionId + '_' + #parameterType")
    List<PromptParameter> findByVersionIdAndParameterType(String versionId, ParameterType parameterType);

    /**
     * Find parameters by version ID and required flag
     */
    @Cacheable(value = "parametersByVersionAndRequired", key = "#versionId + '_' + #required")
    List<PromptParameter> findByVersionIdAndRequired(String versionId, boolean required);

    /**
     * Find parameters by name pattern
     */
    List<PromptParameter> findByNameContaining(String namePattern);

    /**
     * Find usage of a parameter name across versions
     */
    @Query("SELECT p FROM PromptParameter p WHERE p.name = :name")
    List<PromptParameter> findParameterUsage(@Param("name") String name);

    /**
     * Find common parameters across multiple versions
     */
    @Query("SELECT p.name FROM PromptParameter p WHERE p.version.id IN :versionIds " +
            "GROUP BY p.name HAVING COUNT(DISTINCT p.version.id) = :count")
    List<String> findCommonParameters(
            @Param("versionIds") Set<String> versionIds,
            @Param("count") long count);

    /**
     * Find required parameters with default values
     */
    @Query("SELECT p FROM PromptParameter p WHERE p.required = true AND p.defaultValue IS NOT NULL")
    List<PromptParameter> findRequiredParametersWithDefaults();

    /**
     * Count parameters by version ID
     */
    long countByVersionId(String versionId);

    /**
     * Get most used parameter names
     */
    @Query("SELECT p.name, COUNT(p) FROM PromptParameter p GROUP BY p.name ORDER BY COUNT(p) DESC")
    List<Object[]> getMostUsedParameterNames(Pageable pageable);

    /**
     * Check if all required parameters have validation patterns
     */
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN false ELSE true END FROM PromptParameter p " +
            "WHERE p.required = true AND p.validationPattern IS NULL AND p.version.id = :versionId")
    boolean areAllRequiredParametersValidated(@Param("versionId") String versionId);
}