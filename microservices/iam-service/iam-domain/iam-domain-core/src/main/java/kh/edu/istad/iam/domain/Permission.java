package kh.edu.istad.iam.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** A resource-and-action capability that can be assigned directly or through a role. */
public record Permission(
        Long id, UUID uuid, String name, String resource, String action, String description,
        boolean system, boolean active,
        Instant createdAt, String createdBy, Instant updatedAt, String updatedBy,
        boolean deleted, Instant deletedAt, String deletedBy, long version) {

    public Permission {
        Objects.requireNonNull(uuid, "uuid is required");
        resource = required(resource, "resource", 50);
        action = required(action, "action", 50);
        name = required(name, "name", 100);
        if (!name.equals(resource + ":" + action)) {
            throw new IllegalArgumentException("name must equal resource + ':' + action");
        }
        description = optional(description, "description", 255);
        Objects.requireNonNull(createdAt, "createdAt is required");
        createdBy = required(createdBy, "createdBy", 255);
        Objects.requireNonNull(updatedAt, "updatedAt is required");
        updatedBy = required(updatedBy, "updatedBy", 255);
        if (updatedAt.isBefore(createdAt)) throw new IllegalArgumentException("updatedAt cannot be before createdAt");
        if (deleted != (deletedAt != null)) throw new IllegalArgumentException("deletedAt must be set exactly when deleted is true");
        if (deleted != (deletedBy != null)) throw new IllegalArgumentException("deletedBy must be set exactly when deleted is true");
        if (deleted && active) throw new IllegalArgumentException("A deleted permission cannot be active");
        if (system && deleted) throw new IllegalArgumentException("System permissions cannot be deleted");
        if (deletedBy != null) deletedBy = required(deletedBy, "deletedBy", 255);
        if (version < 0) throw new IllegalArgumentException("version cannot be negative");
    }

    public boolean isSystem() { return system; }
    public boolean isActive() { return active; }
    public boolean isDeleted() { return deleted; }

    public Permission updateDetails(String resource, String action, String description, Instant at, String actor) {
        if (system) throw new IllegalStateException("System permissions cannot be changed");
        if (deleted) throw new IllegalStateException("A deleted permission cannot be changed");
        Objects.requireNonNull(at, "audit timestamp is required");
        if (at.isBefore(updatedAt)) throw new IllegalArgumentException("audit timestamp cannot precede updatedAt");
        String updatedResource = required(resource, "resource", 50);
        String updatedAction = required(action, "action", 50);
        return new Permission(id, uuid, updatedResource + ":" + updatedAction,
                updatedResource, updatedAction, optional(description, "description", 255),
                system, active, createdAt, createdBy, at, required(actor, "actor", 255),
                deleted, deletedAt, deletedBy, version + 1);
    }

    public Permission deactivate(Instant at, String actor) {
        if (system) throw new IllegalStateException("System permissions are protected from deactivation");
        if (deleted) throw new IllegalStateException("A deleted permission cannot be deactivated");
        if (!active) return this;
        return changed(false, false, null, null, at, actor);
    }

    public Permission activate(Instant at, String actor) {
        if (system && !active) throw new IllegalStateException("System permissions cannot be changed");
        if (deleted) throw new IllegalStateException("Restore a deleted permission before activating it");
        if (active) return this;
        return changed(false, true, null, null, at, actor);
    }

    public Permission softDelete(Instant at, String actor) {
        if (system) throw new IllegalStateException("System permissions are protected from deletion");
        if (deleted) throw new IllegalStateException("Permission has already been deleted");
        return changed(true, false, at, actor, at, actor);
    }

    public Permission restore(Instant at, String actor) {
        if (!deleted) throw new IllegalStateException("Permission is not deleted");
        return changed(false, false, null, null, at, actor);
    }

    private Permission changed(boolean deleted, boolean active, Instant deletedAt, String deletedBy, Instant at, String actor) {
        Objects.requireNonNull(at, "audit timestamp is required");
        if (at.isBefore(updatedAt)) throw new IllegalArgumentException("audit timestamp cannot precede updatedAt");
        String auditActor = required(actor, "actor", 255);
        return new Permission(id, uuid, name, resource, action, description, system, active,
                createdAt, createdBy, at, auditActor, deleted, deletedAt, deletedBy, version + 1);
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
