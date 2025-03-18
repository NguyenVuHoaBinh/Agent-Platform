package viettel.dac.identityservice.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import viettel.dac.identityservice.model.Project;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, String> {

    List<Project> findByOrganizationId(String organizationId);

    Optional<Project> findByNameAndOrganizationId(String name, String organizationId);

    boolean existsByNameAndOrganizationId(String name, String organizationId);

    @Query("SELECT p FROM Project p WHERE p.name LIKE %:search% OR p.description LIKE %:search%")
    Page<Project> findBySearchTerm(@Param("search") String search, Pageable pageable);

    @Query("SELECT p FROM Project p JOIN p.users u WHERE u.id = :userId")
    List<Project> findByUserId(@Param("userId") String userId);

    @Query("SELECT p FROM Project p JOIN p.users u WHERE u.id = :userId AND p.organization.id = :organizationId")
    List<Project> findByUserIdAndOrganizationId(
            @Param("userId") String userId,
            @Param("organizationId") String organizationId
    );

    @Query("SELECT p FROM Project p LEFT JOIN FETCH p.users WHERE p.id = :id")
    Optional<Project> findByIdWithUsers(@Param("id") String id);

    @Query("SELECT COUNT(p) > 0 FROM Project p JOIN p.users u WHERE p.id = :projectId AND u.id = :userId")
    boolean isMember(@Param("projectId") String projectId, @Param("userId") String userId);
}