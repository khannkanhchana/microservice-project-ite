package kh.edu.istad.iam.persistence.adapter;

import kh.edu.istad.iam.application.port.out.RolePermissionAssignmentRepository;
import kh.edu.istad.iam.domain.RolePermissionAssignment;
import kh.edu.istad.iam.persistence.repository.PermissionJpaRepository;
import kh.edu.istad.iam.persistence.repository.RoleJpaRepository;
import kh.edu.istad.iam.persistence.repository.RolePermissionAssignmentJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
@Transactional
public class RolePermissionAssignmentPersistenceAdapter implements RolePermissionAssignmentRepository {
    private final RolePermissionAssignmentJpaRepository assignments;
    private final RoleJpaRepository roles;
    private final PermissionJpaRepository permissions;

    public RolePermissionAssignmentPersistenceAdapter(RolePermissionAssignmentJpaRepository assignments,
                                                      RoleJpaRepository roles, PermissionJpaRepository permissions) {
        this.assignments = assignments;
        this.roles = roles;
        this.permissions = permissions;
    }

    @Override
    public RolePermissionAssignment save(RolePermissionAssignment assignment) {
        return toDomain(assignments.save(toEntity(assignment)));
    }

    @Override
    public List<RolePermissionAssignment> saveAll(Collection<RolePermissionAssignment> assignmentList) {
        return assignments.saveAll(assignmentList.stream().map(this::toEntity).toList()).stream()
                .map(RolePermissionAssignmentPersistenceAdapter::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RolePermissionAssignment> findByRoleUuids(Collection<UUID> roleUuids) {
        if (roleUuids.isEmpty()) return List.of();
        return assignments.findByRoleUuids(roleUuids).stream().map(RolePermissionAssignmentPersistenceAdapter::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RolePermissionAssignment> findActiveByRoleUuidAndPermissionUuid(UUID roleUuid, UUID permissionUuid, Instant at) {
        return assignments.findActive(roleUuid, permissionUuid, at).stream().map(RolePermissionAssignmentPersistenceAdapter::toDomain).toList();
    }

    private kh.edu.istad.iam.persistence.entity.RolePermissionAssignment toEntity(RolePermissionAssignment model) {
        var entity = new kh.edu.istad.iam.persistence.entity.RolePermissionAssignment();
        entity.setId(model.getId()); entity.setRole(roles.getReferenceById(model.getRoleId()));
        entity.setPermission(permissions.getReferenceById(model.getPermissionId()));
        entity.setAssignedAt(model.getAssignedAt()); entity.setAssignedBy(model.getAssignedBy());
        entity.setExpiresAt(model.getExpiresAt()); entity.setRevokedAt(model.getRevokedAt());
        entity.setRevokedBy(model.getRevokedBy()); entity.setReason(model.getReason());
        return entity;
    }

    private static RolePermissionAssignment toDomain(kh.edu.istad.iam.persistence.entity.RolePermissionAssignment entity) {
        return RolePermissionAssignment.reconstitute(entity.getId(), entity.getRole().getId(), entity.getPermission().getId(),
                entity.getAssignedAt(), entity.getAssignedBy(), entity.getExpiresAt(), entity.getRevokedAt(),
                entity.getRevokedBy(), entity.getReason());
    }
}
