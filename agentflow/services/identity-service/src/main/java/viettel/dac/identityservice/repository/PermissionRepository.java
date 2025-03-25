package viettel.dac.identityservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import viettel.dac.identityservice.entity.Permission;

import java.util.List;
import java.util.Optional;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, String> {

    Optional<Permission> findByName(String name);

    @Query("SELECT p FROM Permission p JOIN p.roles r WHERE r.name = :roleName")
    List<Permission> findByRoleName(@Param("roleName") String roleName);

    @Query("SELECT DISTINCT perm FROM Permission perm " +
            "JOIN perm.roles role " +
            "JOIN role.userRoles ur " +
            "WHERE ur.user.id = :userId")
    List<Permission> findAllByUserId(@Param("userId") String userId);

    @Query("SELECT DISTINCT perm FROM Permission perm " +
            "JOIN perm.roles role " +
            "JOIN role.userRoles ur " +
            "WHERE ur.user.id = :userId AND ur.organization.id = :organizationId")
    List<Permission> findAllByUserIdAndOrganizationId(
            @Param("userId") String userId,
            @Param("organizationId") String organizationId);

    @Query("SELECT DISTINCT perm FROM Permission perm " +
            "JOIN perm.roles role " +
            "JOIN role.userRoles ur " +
            "WHERE ur.user.id = :userId AND ur.project.id = :projectId")
    List<Permission> findAllByUserIdAndProjectId(
            @Param("userId") String userId,
            @Param("projectId") String projectId);
}