package viettel.dac.promptservice.repository.jpa;


import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import viettel.dac.promptservice.model.entity.PromptParameter;
import viettel.dac.promptservice.model.enums.ParameterType;

import java.util.List;
import java.util.Optional;

/**
 * Repository for prompt parameters
 */
@Repository
public interface PromptParameterRepository extends JpaRepository<PromptParameter, String> {

    @Override
    @Cacheable(value = "promptParameters", key = "#id")
    Optional<PromptParameter> findById(String id);

    @Override
    @CacheEvict(value = "promptParameters", key = "#entity.id")
    <S extends PromptParameter> S save(S entity);

    @Override
    @CacheEvict(value = "promptParameters", key = "#id")
    void deleteById(String id);

    List<PromptParameter> findByVersionId(String versionId);

    Optional<PromptParameter> findByVersionIdAndName(String versionId, String name);

    List<PromptParameter> findByVersionIdAndParameterType(String versionId, ParameterType parameterType);

    List<PromptParameter> findByVersionIdAndRequired(String versionId, boolean required);
}
