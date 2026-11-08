package kh.edu.istad.iam.domain;

import java.time.Instant;
import java.util.Objects;

/** Assignment lifecycle and audit data shared by the three IAM link models. */
public abstract class AuditedAssignment {

    private final Long id;
    private final Instant assignedAt;
    private final String assignedBy;
    private final Instant expiresAt;
    private String reason;
    private Instant revokedAt;
    private String revokedBy;

    protected AuditedAssignment(Long id, Instant assignedAt, String assignedBy, Instant expiresAt, String reason) {
        this.id = id;
        this.assignedAt = Objects.requireNonNull(assignedAt, "assignedAt is required");
        this.assignedBy = required(assignedBy, "assignedBy");
        if (expiresAt != null && !expiresAt.isAfter(assignedAt)) {
            throw new IllegalArgumentException("expiresAt must be after assignedAt");
        }
        this.expiresAt = expiresAt;
        this.reason = optional(reason);
    }

    public final void revoke(Instant revokedAt, String revokedBy) {
        revoke(revokedAt, revokedBy, null);
    }

    public final void revoke(Instant revokedAt, String revokedBy, String reason) {
        Objects.requireNonNull(revokedAt, "revokedAt is required");
        String actor = required(revokedBy, "revokedBy");
        if (isRevoked()) {
            throw new IllegalStateException("Assignment has already been revoked");
        }
        if (revokedAt.isBefore(assignedAt)) {
            throw new IllegalArgumentException("revokedAt cannot be before assignedAt");
        }
        this.revokedAt = revokedAt;
        this.revokedBy = actor;
        if (reason != null && !reason.isBlank()) this.reason = optional(reason);
    }

    public final boolean isRevoked() {
        return revokedAt != null;
    }

    /** Checks whether this assignment grants an effect at the supplied instant. */
    public final boolean isActiveAt(Instant at) {
        Objects.requireNonNull(at, "evaluation time is required");
        return !at.isBefore(assignedAt)
                && (expiresAt == null || at.isBefore(expiresAt))
                && (revokedAt == null || at.isBefore(revokedAt));
    }

    public final boolean isActive() {
        return isActiveAt(Instant.now());
    }

    public final Long getId() { return id; }
    public final Instant getAssignedAt() { return assignedAt; }
    public final String getAssignedBy() { return assignedBy; }
    public final Instant getExpiresAt() { return expiresAt; }
    public final String getReason() { return reason; }
    public final Instant getRevokedAt() { return revokedAt; }
    public final String getRevokedBy() { return revokedBy; }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        String normalized = value.trim();
        if (normalized.length() > 100) throw new IllegalArgumentException(name + " cannot exceed 100 characters");
        return normalized;
    }

    private static String optional(String value) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim();
        if (normalized.length() > 255) throw new IllegalArgumentException("reason cannot exceed 255 characters");
        return normalized;
    }
}
