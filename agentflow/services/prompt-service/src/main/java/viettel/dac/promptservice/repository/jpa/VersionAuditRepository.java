package viettel.dac.promptservice.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import viettel.dac.promptservice.model.entity.VersionAuditEntry;

import java.util.List;

@Repository
public interface VersionAuditRepository extends JpaRepository<VersionAuditEntry, String> {

    /**
     * Find audit entries for a specific version, ordered by performed time descending
     *
     * @param versionId The version ID
     * @return List of audit entries
     */
    List<VersionAuditEntry> findByVersionIdOrderByPerformedAtDesc(String versionId);

    /**
     * Find audit trail for a version, including entries for related versions
     *
     * @param versionId The version ID
     * @return List of audit entries
     */
    @Query("SELECT e FROM VersionAuditEntry e WHERE e.version.id = :versionId OR e.referenceVersionId = :versionId ORDER BY e.performedAt DESC")
    List<VersionAuditEntry> findAuditTrailForVersion(@Param("versionId") String versionId);

    /**
     * Find audit entries by action type
     *
     * @param actionType The action type
     * @return List of audit entries
     */
    List<VersionAuditEntry> findByActionType(VersionAuditEntry.AuditActionType actionType);
}