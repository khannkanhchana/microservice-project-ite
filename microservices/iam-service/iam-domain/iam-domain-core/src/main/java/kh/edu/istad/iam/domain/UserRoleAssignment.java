package kh.edu.istad.iam.domain;

import java.time.Instant;
import java.util.Objects;

/** Audited membership of a user in a role. Revocation preserves the row. */
public final class UserRoleAssignment extends AuditedAssignment {

    private final Long userId;
    private final Long roleId;

    public UserRoleAssignment(Long id, User user, Role role, Instant assignedAt,
                              String assignedBy, Instant expiresAt, String reason) {
        this(id, Objects.requireNonNull(user, "user is required").getId(),
                Objects.requireNonNull(role, "role is required").id(), assignedAt, assignedBy, expiresAt, reason);
        if (!role.active() || role.deleted()) throw new IllegalArgumentException("Inactive or deleted roles cannot be assigned");
    }

    private UserRoleAssignment(Long id, Long userId, Long roleId, Instant assignedAt,
                               String assignedBy, Instant expiresAt, String reason) {
        super(id, assignedAt, assignedBy, expiresAt, reason);
        this.userId = Objects.requireNonNull(userId, "user must have a persisted id");
        this.roleId = Objects.requireNonNull(roleId, "role must have a persisted id");
        if (userId <= 0 || roleId <= 0) throw new IllegalArgumentException("userId and roleId must be positive");
    }

    public static UserRoleAssignment reconstitute(Long id, Long userId, Long roleId, Instant assignedAt,
                                                  String assignedBy, Instant expiresAt, Instant revokedAt,
                                                  String revokedBy, String reason) {
        UserRoleAssignment assignment = new UserRoleAssignment(id, userId, roleId, assignedAt, assignedBy, expiresAt, reason);
        if (revokedAt != null) assignment.revoke(revokedAt, revokedBy);
        return assignment;
    }

    public Long getUserId() { return userId; }
    public Long getRoleId() { return roleId; }
}
