package kh.edu.istad.iam.application.port.out;

import kh.edu.istad.iam.domain.UserPermissionAssignment;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface UserPermissionAssignmentRepository {
    UserPermissionAssignment save(UserPermissionAssignment assignment);
    List<UserPermissionAssignment> saveAll(Collection<UserPermissionAssignment> assignments);
    List<UserPermissionAssignment> findByUserUuid(UUID userUuid);
    List<UserPermissionAssignment> findActiveByUserUuidAndPermissionUuid(UUID userUuid, UUID permissionUuid, Instant at);
}
