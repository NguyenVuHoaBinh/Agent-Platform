package viettel.dac.identityservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import viettel.dac.identityservice.entity.Role;
import viettel.dac.identityservice.entity.UserRole;

import java.util.List;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, String> {

    List<UserRole> findByUserId(String userId);

    @Query("SELECT ur FROM UserRole ur WHERE ur.user.id = :userId AND ur.organization.id = :organizationId")
    List<UserRole> findByUserIdAndOrganizationId(@Param("userId") String userId, @Param("organizationId") String organizationId);

    @Query("SELECT ur FROM UserRole ur WHERE ur.user.id = :userId AND ur.project.id = :projectId")
    List<UserRole> findByUserIdAndProjectId(@Param("userId") String userId, @Param("projectId") String projectId);

    @Query("SELECT ur.role FROM UserRole ur WHERE ur.user.id = :userId AND ur.organization IS NULL AND ur.project IS NULL")
    List<Role> findGlobalRolesByUserId(@Param("userId") String userId);

    @Query("SELECT COUNT(ur) > 0 FROM UserRole ur WHERE ur.user.id = :userId AND ur.role.name = :roleName")
    boolean hasRole(@Param("userId") String userId, @Param("roleName") String roleName);

    @Query("SELECT COUNT(ur) > 0 FROM UserRole ur " +
            "WHERE ur.user.id = :userId AND ur.organization.id = :organizationId AND ur.role.name = :roleName")
    boolean hasOrganizationRole(@Param("userId") String userId,
                                @Param("organizationId") String organizationId,
                                @Param("roleName") String roleName);

    @Query("SELECT COUNT(ur) > 0 FROM UserRole ur " +
            "WHERE ur.user.id = :userId AND ur.project.id = :projectId AND ur.role.name = :roleName")
    boolean hasProjectRole(@Param("userId") String userId,
                           @Param("projectId") String projectId,
                           @Param("roleName") String roleName);

    void deleteByUserIdAndRoleIdAndOrganizationIdAndProjectId(String userId, String roleId,
                                                              String organizationId, String projectId);
}