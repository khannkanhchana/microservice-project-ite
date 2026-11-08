package kh.edu.istad.iam.persistence.adapter;

import kh.edu.istad.iam.application.port.out.UserRoleAssignmentRepository;
import kh.edu.istad.iam.domain.UserRoleAssignment;
import kh.edu.istad.iam.persistence.repository.RoleJpaRepository;
import kh.edu.istad.iam.persistence.repository.UserJpaRepository;
import kh.edu.istad.iam.persistence.repository.UserRoleAssignmentJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
@Transactional
public class UserRoleAssignmentPersistenceAdapter implements UserRoleAssignmentRepository {
    private final UserRoleAssignmentJpaRepository assignments;
    private final UserJpaRepository users;
    private final RoleJpaRepository roles;

    public UserRoleAssignmentPersistenceAdapter(UserRoleAssignmentJpaRepository assignments,
                                                UserJpaRepository users, RoleJpaRepository roles) {
        this.assignments = assignments;
        this.users = users;
        this.roles = roles;
    }

    @Override
    public UserRoleAssignment save(UserRoleAssignment assignment) {
        return toDomain(assignments.save(toEntity(assignment)));
    }

    @Override
    public List<UserRoleAssignment> saveAll(Collection<UserRoleAssignment> assignmentList) {
        return assignments.saveAll(assignmentList.stream().map(this::toEntity).toList()).stream()
                .map(UserRoleAssignmentPersistenceAdapter::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserRoleAssignment> findByUserUuid(UUID userUuid) {
        return assignments.findByUserUuid(userUuid).stream().map(UserRoleAssignmentPersistenceAdapter::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserRoleAssignment> findActiveByUserUuidAndRoleUuid(UUID userUuid, UUID roleUuid, Instant at) {
        return assignments.findActive(userUuid, roleUuid, at).stream().map(UserRoleAssignmentPersistenceAdapter::toDomain).toList();
    }

    private kh.edu.istad.iam.persistence.entity.UserRoleAssignment toEntity(UserRoleAssignment model) {
        var entity = new kh.edu.istad.iam.persistence.entity.UserRoleAssignment();
        entity.setId(model.getId());
        entity.setUser(users.getReferenceById(model.getUserId()));
        entity.setRole(roles.getReferenceById(model.getRoleId()));
        entity.setAssignedAt(model.getAssignedAt()); entity.setAssignedBy(model.getAssignedBy());
        entity.setExpiresAt(model.getExpiresAt()); entity.setRevokedAt(model.getRevokedAt());
        entity.setRevokedBy(model.getRevokedBy()); entity.setReason(model.getReason());
        return entity;
    }

    private static UserRoleAssignment toDomain(kh.edu.istad.iam.persistence.entity.UserRoleAssignment entity) {
        return UserRoleAssignment.reconstitute(entity.getId(), entity.getUser().getId(), entity.getRole().getId(),
                entity.getAssignedAt(), entity.getAssignedBy(), entity.getExpiresAt(), entity.getRevokedAt(),
                entity.getRevokedBy(), entity.getReason());
    }
}
