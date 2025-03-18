package viettel.dac.identityservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import viettel.dac.identityservice.model.Permission;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, String> {

    Optional<Permission> findByName(String name);

    boolean existsByName(String name);

    @Query("SELECT p FROM Permission p JOIN p.roles r WHERE r.id = :roleId")
    Set<Permission> findByRoleId(@Param("roleId") String roleId);

    @Query("SELECT p FROM Permission p JOIN p.roles r JOIN r.users u WHERE u.id = :userId")
    Set<Permission> findByUserId(@Param("userId") String userId);

    @Query("SELECT DISTINCT p FROM Permission p JOIN p.roles r JOIN r.users u WHERE u.username = :username")
    List<Permission> findByUsername(@Param("username") String username);
}