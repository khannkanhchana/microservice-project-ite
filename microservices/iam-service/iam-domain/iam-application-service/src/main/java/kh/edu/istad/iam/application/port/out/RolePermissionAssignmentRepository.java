package kh.edu.istad.iam.application.port.out;

import kh.edu.istad.iam.domain.RolePermissionAssignment;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface RolePermissionAssignmentRepository {
    RolePermissionAssignment save(RolePermissionAssignment assignment);
    List<RolePermissionAssignment> saveAll(Collection<RolePermissionAssignment> assignments);
    List<RolePermissionAssignment> findByRoleUuids(Collection<UUID> roleUuids);
    List<RolePermissionAssignment> findActiveByRoleUuidAndPermissionUuid(UUID roleUuid, UUID permissionUuid, Instant at);
}
