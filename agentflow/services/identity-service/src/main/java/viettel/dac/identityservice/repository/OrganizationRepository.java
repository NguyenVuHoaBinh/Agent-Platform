package viettel.dac.identityservice.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import viettel.dac.identityservice.model.Organization;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, String> {

    Optional<Organization> findByName(String name);

    boolean existsByName(String name);

    @Query("SELECT o FROM Organization o WHERE o.name LIKE %:search%")
    Page<Organization> findBySearchTerm(@Param("search") String search, Pageable pageable);

    @Query("SELECT o FROM Organization o JOIN o.users u WHERE u.id = :userId")
    List<Organization> findByUserId(@Param("userId") String userId);

    @Query("SELECT o FROM Organization o LEFT JOIN FETCH o.projects WHERE o.id = :id")
    Optional<Organization> findByIdWithProjects(@Param("id") String id);

    @Query("SELECT COUNT(o) > 0 FROM Organization o JOIN o.users u WHERE o.id = :organizationId AND u.id = :userId")
    boolean isMember(@Param("organizationId") String organizationId, @Param("userId") String userId);
}