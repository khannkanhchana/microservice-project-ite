package kh.edu.istad.iam.persistence.adapter;

import kh.edu.istad.iam.application.port.out.UserPermissionAssignmentRepository;
import kh.edu.istad.iam.domain.UserPermissionAssignment;
import kh.edu.istad.iam.persistence.repository.PermissionJpaRepository;
import kh.edu.istad.iam.persistence.repository.UserJpaRepository;
import kh.edu.istad.iam.persistence.repository.UserPermissionAssignmentJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
@Transactional
public class UserPermissionAssignmentPersistenceAdapter implements UserPermissionAssignmentRepository {
    private final UserPermissionAssignmentJpaRepository assignments;
    private final UserJpaRepository users;
    private final PermissionJpaRepository permissions;

    public UserPermissionAssignmentPersistenceAdapter(UserPermissionAssignmentJpaRepository assignments,
                                                      UserJpaRepository users, PermissionJpaRepository permissions) {
        this.assignments = assignments;
        this.users = users;
        this.permissions = permissions;
    }

    @Override
    public UserPermissionAssignment save(UserPermissionAssignment assignment) {
        return toDomain(assignments.save(toEntity(assignment)));
    }

    @Override
    public List<UserPermissionAssignment> saveAll(Collection<UserPermissionAssignment> assignmentList) {
        return assignments.saveAll(assignmentList.stream().map(this::toEntity).toList()).stream()
                .map(UserPermissionAssignmentPersistenceAdapter::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserPermissionAssignment> findByUserUuid(UUID userUuid) {
        return assignments.findByUserUuid(userUuid).stream().map(UserPermissionAssignmentPersistenceAdapter::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserPermissionAssignment> findActiveByUserUuidAndPermissionUuid(UUID userUuid, UUID permissionUuid, Instant at) {
        return assignments.findActive(userUuid, permissionUuid, at).stream().map(UserPermissionAssignmentPersistenceAdapter::toDomain).toList();
    }

    private kh.edu.istad.iam.persistence.entity.UserPermissionAssignment toEntity(UserPermissionAssignment model) {
        var entity = new kh.edu.istad.iam.persistence.entity.UserPermissionAssignment();
        entity.setId(model.getId()); entity.setUser(users.getReferenceById(model.getUserId()));
        entity.setPermission(permissions.getReferenceById(model.getPermissionId())); entity.setEffect(model.getEffect());
        entity.setAssignedAt(model.getAssignedAt()); entity.setAssignedBy(model.getAssignedBy());
        entity.setExpiresAt(model.getExpiresAt()); entity.setRevokedAt(model.getRevokedAt());
        entity.setRevokedBy(model.getRevokedBy()); entity.setReason(model.getReason());
        return entity;
    }

    private static UserPermissionAssignment toDomain(kh.edu.istad.iam.persistence.entity.UserPermissionAssignment entity) {
        return UserPermissionAssignment.reconstitute(entity.getId(), entity.getUser().getId(), entity.getPermission().getId(),
                entity.getEffect(), entity.getAssignedAt(), entity.getAssignedBy(), entity.getExpiresAt(),
                entity.getRevokedAt(), entity.getRevokedBy(), entity.getReason());
    }
}
