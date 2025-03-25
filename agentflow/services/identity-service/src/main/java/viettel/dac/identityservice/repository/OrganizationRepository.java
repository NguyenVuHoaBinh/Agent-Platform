package viettel.dac.identityservice.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import viettel.dac.identityservice.entity.Organization;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, String> {

    boolean existsByName(String name);

    Optional<Organization> findByName(String name);

    @Query("SELECT o FROM Organization o WHERE o.parent.id = :parentId")
    List<Organization> findByParentId(@Param("parentId") String parentId);

    @Query("SELECT DISTINCT o FROM Organization o JOIN o.userRoles ur WHERE ur.user.id = :userId")
    Page<Organization> findOrganizationsByUserId(@Param("userId") String userId, Pageable pageable);

    @Query("SELECT o FROM Organization o WHERE LOWER(o.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
            "OR LOWER(o.displayName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
            "OR LOWER(o.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<Organization> findBySearchTerm(@Param("searchTerm") String searchTerm, Pageable pageable);

    @Query("SELECT o FROM Organization o WHERE o.status = :status")
    Page<Organization> findByStatus(@Param("status") Organization.OrganizationStatus status, Pageable pageable);
}