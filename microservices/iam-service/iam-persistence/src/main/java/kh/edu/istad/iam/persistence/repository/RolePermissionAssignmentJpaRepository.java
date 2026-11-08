package kh.edu.istad.iam.persistence.repository;

import kh.edu.istad.iam.persistence.entity.RolePermissionAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface RolePermissionAssignmentJpaRepository extends JpaRepository<RolePermissionAssignment, Long> {
    @Query("select a from RolePermissionAssignment a where a.role.uuid in :roleUuids")
    List<RolePermissionAssignment> findByRoleUuids(@Param("roleUuids") Collection<UUID> roleUuids);

    @Query("select a from RolePermissionAssignment a where a.role.uuid = :roleUuid and a.permission.uuid = :permissionUuid "
            + "and a.assignedAt <= :at and (a.expiresAt is null or a.expiresAt > :at) "
            + "and (a.revokedAt is null or a.revokedAt > :at)")
    List<RolePermissionAssignment> findActive(@Param("roleUuid") UUID roleUuid,
                                              @Param("permissionUuid") UUID permissionUuid,
                                              @Param("at") Instant at);
}
