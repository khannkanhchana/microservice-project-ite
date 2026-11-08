package kh.edu.istad.iam.domain;

import java.time.Instant;
import java.util.Objects;

/** Audited permission granted to a role. Revocation preserves the row. */
public final class RolePermissionAssignment extends AuditedAssignment {

    private final Long roleId;
    private final Long permissionId;

    public RolePermissionAssignment(Long id, Role role, Permission permission, Instant assignedAt,
                                    String assignedBy, Instant expiresAt, String reason) {
        this(id, Objects.requireNonNull(role, "role is required").id(),
                Objects.requireNonNull(permission, "permission is required").id(), assignedAt, assignedBy, expiresAt, reason);
        if (!role.active() || role.deleted()) throw new IllegalArgumentException("Inactive or deleted roles cannot be assigned");
        if (!permission.active() || permission.deleted()) throw new IllegalArgumentException("Inactive or deleted permissions cannot be assigned");
    }

    private RolePermissionAssignment(Long id, Long roleId, Long permissionId, Instant assignedAt,
                                     String assignedBy, Instant expiresAt, String reason) {
        super(id, assignedAt, assignedBy, expiresAt, reason);
        this.roleId = Objects.requireNonNull(roleId, "role must have a persisted id");
        this.permissionId = Objects.requireNonNull(permissionId, "permission must have a persisted id");
        if (roleId <= 0 || permissionId <= 0) throw new IllegalArgumentException("roleId and permissionId must be positive");
    }

    public static RolePermissionAssignment reconstitute(Long id, Long roleId, Long permissionId,
                                                        Instant assignedAt, String assignedBy,
                                                        Instant expiresAt, Instant revokedAt,
                                                        String revokedBy, String reason) {
        RolePermissionAssignment assignment = new RolePermissionAssignment(id, roleId, permissionId,
                assignedAt, assignedBy, expiresAt, reason);
        if (revokedAt != null) assignment.revoke(revokedAt, revokedBy);
        return assignment;
    }

    public Long getRoleId() { return roleId; }
    public Long getPermissionId() { return permissionId; }
}
