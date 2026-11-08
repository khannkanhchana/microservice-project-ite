package kh.edu.istad.iam.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** A named security role. Membership and permissions are represented by audited assignments. */
public record Role(
        Long id, UUID uuid, String name, String description,
        boolean system, boolean active,
        Instant createdAt, String createdBy, Instant updatedAt, String updatedBy,
        boolean deleted, Instant deletedAt, String deletedBy, long version) {

    public Role {
        Objects.requireNonNull(uuid, "uuid is required");
        name = required(name, "name", 50);
        description = optional(description, "description", 255);
        Objects.requireNonNull(createdAt, "createdAt is required");
        createdBy = required(createdBy, "createdBy", 255);
        Objects.requireNonNull(updatedAt, "updatedAt is required");
        updatedBy = required(updatedBy, "updatedBy", 255);
        if (updatedAt.isBefore(createdAt)) throw new IllegalArgumentException("updatedAt cannot be before createdAt");
        if (deleted != (deletedAt != null)) throw new IllegalArgumentException("deletedAt must be set exactly when deleted is true");
        if (deleted != (deletedBy != null)) throw new IllegalArgumentException("deletedBy must be set exactly when deleted is true");
        if (deleted && active) throw new IllegalArgumentException("A deleted role cannot be active");
        if (system && deleted) throw new IllegalArgumentException("System roles cannot be deleted");
        if (deletedBy != null) deletedBy = required(deletedBy, "deletedBy", 255);
        if (version < 0) throw new IllegalArgumentException("version cannot be negative");
    }

    public boolean isSystem() { return system; }
    public boolean isActive() { return active; }
    public boolean isDeleted() { return deleted; }

    public Role updateDetails(String name, String description, Instant at, String actor) {
        if (system) throw new IllegalStateException("System roles cannot be changed");
        if (deleted) throw new IllegalStateException("A deleted role cannot be changed");
        Objects.requireNonNull(at, "audit timestamp is required");
        if (at.isBefore(updatedAt)) throw new IllegalArgumentException("audit timestamp cannot precede updatedAt");
        return new Role(id, uuid, required(name, "name", 50), optional(description, "description", 255),
                system, active, createdAt, createdBy, at, required(actor, "actor", 255),
                deleted, deletedAt, deletedBy, version + 1);
    }

    public Role deactivate(Instant at, String actor) {
        if (system) throw new IllegalStateException("System roles are protected from deactivation");
        if (deleted) throw new IllegalStateException("A deleted role cannot be deactivated");
        if (!active) return this;
        return changed(false, false, null, null, at, actor);
    }

    public Role activate(Instant at, String actor) {
        if (system && !active) throw new IllegalStateException("System roles cannot be changed");
        if (deleted) throw new IllegalStateException("Restore a deleted role before activating it");
        if (active) return this;
        return changed(false, true, null, null, at, actor);
    }

    public Role softDelete(Instant at, String actor) {
        if (system) throw new IllegalStateException("System roles are protected from deletion");
        if (deleted) throw new IllegalStateException("Role has already been deleted");
        return changed(true, false, at, actor, at, actor);
    }

    public Role restore(Instant at, String actor) {
        if (!deleted) throw new IllegalStateException("Role is not deleted");
        return changed(false, false, null, null, at, actor);
    }

    private Role changed(boolean deleted, boolean active, Instant deletedAt, String deletedBy, Instant at, String actor) {
        Objects.requireNonNull(at, "audit timestamp is required");
        if (at.isBefore(updatedAt)) throw new IllegalArgumentException("audit timestamp cannot precede updatedAt");
        String auditActor = required(actor, "actor", 255);
        return new Role(id, uuid, name, description, system, active, createdAt, createdBy,
                at, auditActor, deleted, deletedAt, deletedBy, version + 1);
    }

    private static String required(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        String normalized = value.trim();
        if (normalized.length() > maxLength) throw new IllegalArgumentException(field + " cannot exceed " + maxLength + " characters");
        return normalized;
    }

    private static String optional(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim();
        if (normalized.length() > maxLength) throw new IllegalArgumentException(field + " cannot exceed " + maxLength + " characters");
        return normalized;
    }
}
