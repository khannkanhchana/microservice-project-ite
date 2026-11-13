package kh.edu.istad.iam.infrastructure.admin.rbac;

import kh.edu.istad.iam.application.command.*;
import kh.edu.istad.iam.application.query.PermissionFilterQuery;
import kh.edu.istad.iam.application.query.RoleFilterQuery;
import kh.edu.istad.iam.application.result.*;
import kh.edu.istad.iam.application.usecase.*;
import kh.edu.istad.iam.infrastructure.security.LiveAuthorityResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Role and permission administration use cases: CRUD, activation, and permission-to-role assignment.
 * Permissions are resource-based: {@code name = resource:action}. System roles/permissions (seeded for the
 * administration console) cannot be renamed, deactivated or deleted.
 */
public final class RolePermissionAdministration {
    private RolePermissionAdministration() { }

    private static final Pattern ROLE_NAME = Pattern.compile("^[A-Za-z][A-Za-z0-9_-]{1,49}$");
    private static final Pattern PART = Pattern.compile("^[a-z][a-z0-9_.-]{0,49}$");

    static final RowMapper<RoleResult> ROLE = (rs, i) -> new RoleResult(rs.getObject("uuid", UUID.class),
            rs.getString("name"), rs.getString("description"), rs.getBoolean("is_system"), rs.getBoolean("is_active"),
            rs.getBoolean("is_deleted"), inst(rs.getTimestamp("created_at")), rs.getString("created_by"),
            inst(rs.getTimestamp("updated_at")), rs.getString("updated_by"), inst(rs.getTimestamp("deleted_at")),
            rs.getString("deleted_by"), rs.getLong("version"));

    static final RowMapper<PermissionResult> PERMISSION = (rs, i) -> new PermissionResult(
            rs.getObject("uuid", UUID.class), rs.getString("name"), rs.getString("resource"), rs.getString("action"),
            rs.getString("description"), rs.getBoolean("is_system"), rs.getBoolean("is_active"),
            rs.getBoolean("is_deleted"), inst(rs.getTimestamp("created_at")), rs.getString("created_by"),
            inst(rs.getTimestamp("updated_at")), rs.getString("updated_by"), inst(rs.getTimestamp("deleted_at")),
            rs.getString("deleted_by"), rs.getLong("version"));

    /** Shared persistence helpers. */
    @Component
    public static class Store {
        final JdbcTemplate jdbc;
        final LiveAuthorityResolver authorities;

        public Store(JdbcTemplate jdbc, LiveAuthorityResolver authorities) {
            this.jdbc = jdbc;
            this.authorities = authorities;
        }

        Map<String, Object> role(UUID uuid) {
            List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM roles WHERE uuid = ? AND is_deleted = FALSE", uuid);
            if (rows.isEmpty()) throw new IllegalArgumentException("Role was not found.");
            return rows.getFirst();
        }

        Map<String, Object> permission(UUID uuid) {
            List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM permissions WHERE uuid = ? AND is_deleted = FALSE", uuid);
            if (rows.isEmpty()) throw new IllegalArgumentException("Permission was not found.");
            return rows.getFirst();
        }

        RoleResult getRole(UUID uuid) {
            return jdbc.queryForObject("SELECT * FROM roles WHERE uuid = ?", ROLE, uuid);
        }

        PermissionResult getPermission(UUID uuid) {
            return jdbc.queryForObject("SELECT * FROM permissions WHERE uuid = ?", PERMISSION, uuid);
        }

        void refreshAuthorities() {
            authorities.evict(null); // role/permission changes can affect many users
        }
    }

    // ---- Roles ------------------------------------------------------------------------------------------------

    @Service
    public static class CreateRole implements CreateRoleUseCase {
        private final Store store;
        public CreateRole(Store store) { this.store = store; }

        @Override
        public RoleResult execute(CreateRoleCommand c) {
            String name = roleName(c.name());
            if (store.jdbc.queryForObject("SELECT count(*) FROM roles WHERE upper(name) = ?", Long.class, name) > 0) {
                throw new IllegalArgumentException("A role named " + name + " already exists.");
            }
            UUID uuid = UUID.randomUUID();
            store.jdbc.update("""
                    INSERT INTO roles (uuid, name, description, is_system, is_active, created_at, created_by, updated_at, updated_by)
                    VALUES (?, ?, ?, FALSE, TRUE, now(), ?, now(), ?)""", uuid, name, blankToNull(c.description()),
                    c.createdBy(), c.createdBy());
            return store.getRole(uuid);
        }
    }

    @Service
    public static class ListRoles implements ListRolesUseCase {
        private final Store store;
        public ListRoles(Store store) { this.store = store; }

        @Override
        public PageResult<RoleSummary> execute(RoleFilterQuery q) {
            StringBuilder where = new StringBuilder(" WHERE is_deleted = ?");
            List<Object> args = new ArrayList<>(List.of(Boolean.TRUE.equals(q.deleted())));
            if (q.search() != null && !q.search().isBlank()) {
                String like = "%" + q.search().trim().toLowerCase(Locale.ROOT) + "%";
                where.append(" AND (lower(name) LIKE ? OR lower(coalesce(description, '')) LIKE ?)");
                args.addAll(List.of(like, like));
            }
            if (q.system() != null) { where.append(" AND is_system = ?"); args.add(q.system()); }
            if (q.active() != null) { where.append(" AND is_active = ?"); args.add(q.active()); }
            String column = "createdAt".equals(q.sortBy()) ? "created_at" : "name";
            long total = store.jdbc.queryForObject("SELECT count(*) FROM roles" + where, Long.class, args.toArray());
            args.add(q.size());
            args.add((long) q.page() * q.size());
            List<RoleSummary> rows = store.jdbc.query("SELECT * FROM roles" + where + " ORDER BY " + column + " "
                    + q.sortDirection().name() + " LIMIT ? OFFSET ?",
                    (rs, i) -> new RoleSummary(rs.getObject("uuid", UUID.class), rs.getString("name"),
                            rs.getString("description"), rs.getBoolean("is_system"), rs.getBoolean("is_active"),
                            inst(rs.getTimestamp("created_at"))), args.toArray());
            return new PageResult<>(rows, q.page(), q.size(), total, (int) Math.ceil(total / (double) q.size()));
        }
    }

    @Service
    public static class GetRole implements GetRoleUseCase {
        private final Store store;
        public GetRole(Store store) { this.store = store; }

        @Override
        public RoleResult execute(UUID roleUuid) {
            store.role(roleUuid);
            return store.getRole(roleUuid);
        }
    }

    @Service
    public static class UpdateRole implements UpdateRoleUseCase {
        private final Store store;
        public UpdateRole(Store store) { this.store = store; }

        @Override
        public RoleResult execute(UpdateRoleCommand c) {
            Map<String, Object> role = store.role(c.roleUuid());
            String name = roleName(c.name());
            if ((Boolean) role.get("is_system") && !name.equals(role.get("name"))) {
                throw new IllegalArgumentException("System roles cannot be renamed.");
            }
            if (store.jdbc.queryForObject("SELECT count(*) FROM roles WHERE upper(name) = ? AND uuid <> ?", Long.class,
                    name, c.roleUuid()) > 0) {
                throw new IllegalArgumentException("A role named " + name + " already exists.");
            }
            store.jdbc.update("""
                    UPDATE roles SET name = ?, description = ?, updated_at = now(), updated_by = ?, version = version + 1
                    WHERE uuid = ?""", name, blankToNull(c.description()), c.updatedBy(), c.roleUuid());
            store.refreshAuthorities();
            return store.getRole(c.roleUuid());
        }
    }

    @Service
    public static class DeleteRole implements DeleteRoleUseCase {
        private final Store store;
        public DeleteRole(Store store) { this.store = store; }

        @Override
        @Transactional
        public void execute(UUID roleUuid, Instant at, String actor) {
            Map<String, Object> role = store.role(roleUuid);
            if ((Boolean) role.get("is_system")) throw new IllegalArgumentException("System roles cannot be deleted.");
            Timestamp now = Timestamp.from(at);
            Object id = role.get("id");
            store.jdbc.update("UPDATE user_roles SET revoked_at = ?, revoked_by = ?, reason = 'Role deleted' WHERE role_id = ? AND revoked_at IS NULL", now, actor, id);
            store.jdbc.update("UPDATE role_perms SET revoked_at = ?, revoked_by = ?, reason = 'Role deleted' WHERE role_id = ? AND revoked_at IS NULL", now, actor, id);
            store.jdbc.update("""
                    UPDATE roles SET is_deleted = TRUE, is_active = FALSE, deleted_at = ?, deleted_by = ?,
                        updated_at = ?, updated_by = ?, version = version + 1 WHERE id = ?""", now, actor, now, actor, id);
            store.refreshAuthorities();
        }
    }

    @Service
    public static class ActivateRole implements ActivateRoleUseCase {
        private final Store store;
        public ActivateRole(Store store) { this.store = store; }

        @Override
        public void execute(UUID roleUuid, Instant at, String actor) {
            store.role(roleUuid);
            store.jdbc.update("UPDATE roles SET is_active = TRUE, updated_at = ?, updated_by = ?, version = version + 1 WHERE uuid = ?",
                    Timestamp.from(at), actor, roleUuid);
            store.refreshAuthorities();
        }
    }

    @Service
    public static class DeactivateRole implements DeactivateRoleUseCase {
        private final Store store;
        public DeactivateRole(Store store) { this.store = store; }

        @Override
        public void execute(UUID roleUuid, Instant at, String actor) {
            if ((Boolean) store.role(roleUuid).get("is_system")) throw new IllegalArgumentException("System roles cannot be deactivated.");
            store.jdbc.update("UPDATE roles SET is_active = FALSE, updated_at = ?, updated_by = ?, version = version + 1 WHERE uuid = ?",
                    Timestamp.from(at), actor, roleUuid);
            store.refreshAuthorities();
        }
    }

    @Service
    public static class AssignPermissionsToRole implements AssignPermissionsToRoleUseCase {
        private final Store store;
        public AssignPermissionsToRole(Store store) { this.store = store; }

        @Override
        @Transactional
        public List<PermissionAssignmentResult> execute(AssignPermissionsToRoleCommand c) {
            Object roleId = store.role(c.roleUuid()).get("id");
            if (c.permissionUuids().isEmpty()) throw new IllegalArgumentException("Select at least one permission.");
            Timestamp expires = c.expiresAt() == null ? null : Timestamp.from(c.expiresAt());
            List<PermissionAssignmentResult> results = new ArrayList<>();
            for (UUID permissionUuid : c.permissionUuids()) {
                Map<String, Object> permission = store.permission(permissionUuid);
                List<Map<String, Object>> existing = store.jdbc.queryForList("""
                        SELECT * FROM role_perms WHERE role_id = ? AND permission_id = ? AND revoked_at IS NULL
                          AND (expires_at IS NULL OR expires_at > now())""", roleId, permission.get("id"));
                Map<String, Object> row = existing.isEmpty() ? store.jdbc.queryForMap("""
                        INSERT INTO role_perms (role_id, permission_id, assigned_at, assigned_by, expires_at, reason)
                        VALUES (?, ?, now(), ?, ?, ?) RETURNING *""", roleId, permission.get("id"), c.assignedBy(),
                        expires, c.reason()) : existing.getFirst();
                results.add(new PermissionAssignmentResult(((Number) row.get("id")).longValue(), permissionUuid,
                        (String) permission.get("name"), UserAdministration.ts(row.get("assigned_at")),
                        (String) row.get("assigned_by"), UserAdministration.ts(row.get("expires_at")), null, null,
                        (String) row.get("reason")));
            }
            store.refreshAuthorities();
            return results;
        }
    }

    @Service
    public static class RevokePermissionFromRole implements RevokePermissionFromRoleUseCase {
        private final Store store;
        public RevokePermissionFromRole(Store store) { this.store = store; }

        @Override
        public void execute(UUID roleUuid, UUID permissionUuid, String reason, String revokedBy, Instant revokedAt) {
            Map<String, Object> role = store.role(roleUuid);
            Map<String, Object> permission = store.permission(permissionUuid);
            if ("ADMIN".equals(role.get("name")) && (Boolean) permission.get("is_system")) {
                throw new IllegalArgumentException("System permissions cannot be removed from the ADMIN role.");
            }
            int updated = store.jdbc.update("""
                    UPDATE role_perms SET revoked_at = ?, revoked_by = ?, reason = coalesce(?, reason)
                    WHERE role_id = ? AND permission_id = ? AND revoked_at IS NULL""",
                    Timestamp.from(revokedAt), revokedBy, reason, role.get("id"), permission.get("id"));
            if (updated == 0) throw new IllegalArgumentException("This permission is not assigned to the role.");
            store.refreshAuthorities();
        }
    }

    // ---- Permissions ------------------------------------------------------------------------------------------

    @Service
    public static class CreatePermission implements CreatePermissionUseCase {
        private final Store store;
        public CreatePermission(Store store) { this.store = store; }

        @Override
        public PermissionResult execute(CreatePermissionCommand c) {
            String resource = part("Resource", c.resource());
            String action = part("Action", c.action());
            if (store.jdbc.queryForObject("SELECT count(*) FROM permissions WHERE resource = ? AND action = ?", Long.class,
                    resource, action) > 0) {
                throw new IllegalArgumentException("Permission " + resource + ":" + action + " already exists.");
            }
            UUID uuid = UUID.randomUUID();
            store.jdbc.update("""
                    INSERT INTO permissions (uuid, name, resource, action, description, is_system, is_active,
                        created_at, created_by, updated_at, updated_by)
                    VALUES (?, ?, ?, ?, ?, FALSE, TRUE, now(), ?, now(), ?)""", uuid, resource + ":" + action, resource,
                    action, blankToNull(c.description()), c.createdBy(), c.createdBy());
            return store.getPermission(uuid);
        }
    }

    @Service
    public static class ListPermissions implements ListPermissionsUseCase {
        private final Store store;
        public ListPermissions(Store store) { this.store = store; }

        @Override
        public PageResult<PermissionResult> execute(PermissionFilterQuery q) {
            StringBuilder where = new StringBuilder(" WHERE is_deleted = ?");
            List<Object> args = new ArrayList<>(List.of(Boolean.TRUE.equals(q.deleted())));
            if (q.search() != null && !q.search().isBlank()) {
                String like = "%" + q.search().trim().toLowerCase(Locale.ROOT) + "%";
                where.append(" AND (lower(name) LIKE ? OR lower(coalesce(description, '')) LIKE ?)");
                args.addAll(List.of(like, like));
            }
            if (q.resource() != null && !q.resource().isBlank()) { where.append(" AND resource = ?"); args.add(q.resource().trim()); }
            if (q.action() != null && !q.action().isBlank()) { where.append(" AND action = ?"); args.add(q.action().trim()); }
            if (q.system() != null) { where.append(" AND is_system = ?"); args.add(q.system()); }
            if (q.active() != null) { where.append(" AND is_active = ?"); args.add(q.active()); }
            String column = switch (q.sortBy()) {
                case "resource" -> "resource, action";
                case "createdAt" -> "created_at";
                default -> "name";
            };
            long total = store.jdbc.queryForObject("SELECT count(*) FROM permissions" + where, Long.class, args.toArray());
            args.add(q.size());
            args.add((long) q.page() * q.size());
            List<PermissionResult> rows = store.jdbc.query("SELECT * FROM permissions" + where + " ORDER BY " + column
                    + " " + q.sortDirection().name() + " LIMIT ? OFFSET ?", PERMISSION, args.toArray());
            return new PageResult<>(rows, q.page(), q.size(), total, (int) Math.ceil(total / (double) q.size()));
        }
    }

    @Service
    public static class GetPermission implements GetPermissionUseCase {
        private final Store store;
        public GetPermission(Store store) { this.store = store; }

        @Override
        public PermissionResult execute(UUID permissionUuid) {
            store.permission(permissionUuid);
            return store.getPermission(permissionUuid);
        }
    }

    @Service
    public static class UpdatePermission implements UpdatePermissionUseCase {
        private final Store store;
        public UpdatePermission(Store store) { this.store = store; }

        @Override
        public PermissionResult execute(UpdatePermissionCommand c) {
            Map<String, Object> permission = store.permission(c.permissionUuid());
            String resource = part("Resource", c.resource());
            String action = part("Action", c.action());
            boolean renamed = !resource.equals(permission.get("resource")) || !action.equals(permission.get("action"));
            if ((Boolean) permission.get("is_system") && renamed) {
                throw new IllegalArgumentException("System permissions cannot be renamed.");
            }
            if (renamed && store.jdbc.queryForObject("SELECT count(*) FROM permissions WHERE resource = ? AND action = ? AND uuid <> ?",
                    Long.class, resource, action, c.permissionUuid()) > 0) {
                throw new IllegalArgumentException("Permission " + resource + ":" + action + " already exists.");
            }
            store.jdbc.update("""
                    UPDATE permissions SET name = ?, resource = ?, action = ?, description = ?, updated_at = now(),
                        updated_by = ?, version = version + 1 WHERE uuid = ?""", resource + ":" + action, resource, action,
                    blankToNull(c.description()), c.updatedBy(), c.permissionUuid());
            store.refreshAuthorities();
            return store.getPermission(c.permissionUuid());
        }
    }

    @Service
    public static class DeletePermission implements DeletePermissionUseCase {
        private final Store store;
        public DeletePermission(Store store) { this.store = store; }

        @Override
        @Transactional
        public void execute(UUID permissionUuid, Instant at, String actor) {
            Map<String, Object> permission = store.permission(permissionUuid);
            if ((Boolean) permission.get("is_system")) throw new IllegalArgumentException("System permissions cannot be deleted.");
            Timestamp now = Timestamp.from(at);
            Object id = permission.get("id");
            store.jdbc.update("UPDATE role_perms SET revoked_at = ?, revoked_by = ?, reason = 'Permission deleted' WHERE permission_id = ? AND revoked_at IS NULL", now, actor, id);
            store.jdbc.update("UPDATE user_perms SET revoked_at = ?, revoked_by = ?, reason = 'Permission deleted' WHERE permission_id = ? AND revoked_at IS NULL", now, actor, id);
            store.jdbc.update("""
                    UPDATE permissions SET is_deleted = TRUE, is_active = FALSE, deleted_at = ?, deleted_by = ?,
                        updated_at = ?, updated_by = ?, version = version + 1 WHERE id = ?""", now, actor, now, actor, id);
            store.refreshAuthorities();
        }
    }

    @Service
    public static class ActivatePermission implements ActivatePermissionUseCase {
        private final Store store;
        public ActivatePermission(Store store) { this.store = store; }

        @Override
        public void execute(UUID permissionUuid, Instant at, String actor) {
            store.permission(permissionUuid);
            store.jdbc.update("UPDATE permissions SET is_active = TRUE, updated_at = ?, updated_by = ?, version = version + 1 WHERE uuid = ?",
                    Timestamp.from(at), actor, permissionUuid);
            store.refreshAuthorities();
        }
    }

    @Service
    public static class DeactivatePermission implements DeactivatePermissionUseCase {
        private final Store store;
        public DeactivatePermission(Store store) { this.store = store; }

        @Override
        public void execute(UUID permissionUuid, Instant at, String actor) {
            if ((Boolean) store.permission(permissionUuid).get("is_system")) {
                throw new IllegalArgumentException("System permissions cannot be deactivated.");
            }
            store.jdbc.update("UPDATE permissions SET is_active = FALSE, updated_at = ?, updated_by = ?, version = version + 1 WHERE uuid = ?",
                    Timestamp.from(at), actor, permissionUuid);
            store.refreshAuthorities();
        }
    }

    // ---- helpers ----------------------------------------------------------------------------------------------

    private static String roleName(String value) {
        String name = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (!ROLE_NAME.matcher(name).matches()) {
            throw new IllegalArgumentException("Role name must be 2-50 characters: letters, digits, '_' or '-', starting with a letter.");
        }
        return name;
    }

    private static String part(String label, String value) {
        String part = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (!PART.matcher(part).matches()) {
            throw new IllegalArgumentException(label + " must start with a letter and use lowercase letters, digits, '_', '-' or '.'.");
        }
        return part;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static Instant inst(Timestamp value) {
        return value == null ? null : value.toInstant();
    }
}
