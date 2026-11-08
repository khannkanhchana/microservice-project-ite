package kh.edu.istad.iam.domain;

import java.time.Instant;
import java.util.Objects;

/** Audited permission granted directly to a user. Revocation preserves the row. */
public final class UserPermissionAssignment extends AuditedAssignment {

    private final Long userId;
    private final Long permissionId;
    private final AssignmentEffect effect;

    public UserPermissionAssignment(Long id, User user, Permission permission, AssignmentEffect effect,
                                    Instant assignedAt, String assignedBy, Instant expiresAt, String reason) {
        this(id, Objects.requireNonNull(user, "user is required").getId(),
                Objects.requireNonNull(permission, "permission is required").id(), effect,
                assignedAt, assignedBy, expiresAt, reason);
        if (!permission.active() || permission.deleted()) throw new IllegalArgumentException("Inactive or deleted permissions cannot be assigned");
    }

    private UserPermissionAssignment(Long id, Long userId, Long permissionId, AssignmentEffect effect,
                                     Instant assignedAt, String assignedBy, Instant expiresAt, String reason) {
        super(id, assignedAt, assignedBy, expiresAt, reason);
        this.userId = Objects.requireNonNull(userId, "user must have a persisted id");
        this.permissionId = Objects.requireNonNull(permissionId, "permission must have a persisted id");
        if (userId <= 0 || permissionId <= 0) throw new IllegalArgumentException("userId and permissionId must be positive");
        this.effect = Objects.requireNonNull(effect, "effect is required");
    }

    public static UserPermissionAssignment reconstitute(Long id, Long userId, Long permissionId,
                                                        AssignmentEffect effect, Instant assignedAt,
                                                        String assignedBy, Instant expiresAt,
                                                        Instant revokedAt, String revokedBy, String reason) {
        UserPermissionAssignment assignment = new UserPermissionAssignment(id, userId, permissionId,
                effect, assignedAt, assignedBy, expiresAt, reason);
        if (revokedAt != null) assignment.revoke(revokedAt, revokedBy);
        return assignment;
    }

    public Long getUserId() { return userId; }
    public Long getPermissionId() { return permissionId; }
    public AssignmentEffect getEffect() { return effect; }
}
