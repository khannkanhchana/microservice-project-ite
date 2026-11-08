package kh.edu.istad.iam.application.port.out;

import kh.edu.istad.iam.domain.UserRoleAssignment;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface UserRoleAssignmentRepository {
    UserRoleAssignment save(UserRoleAssignment assignment);
    List<UserRoleAssignment> saveAll(Collection<UserRoleAssignment> assignments);
    List<UserRoleAssignment> findByUserUuid(UUID userUuid);
    List<UserRoleAssignment> findActiveByUserUuidAndRoleUuid(UUID userUuid, UUID roleUuid, Instant at);
}
