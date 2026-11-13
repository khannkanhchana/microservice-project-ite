package kh.edu.istad.iam.infrastructure.admin.rbac;

import kh.edu.istad.iam.application.command.AssignRolesToUserCommand;
import kh.edu.istad.iam.application.command.CreateUserCommand;
import kh.edu.istad.iam.application.command.GrantPermissionsToUserCommand;
import kh.edu.istad.iam.application.port.out.PasswordHasher;
import kh.edu.istad.iam.application.port.out.UserRepository;
import kh.edu.istad.iam.application.query.UserFilterQuery;
import kh.edu.istad.iam.application.result.*;
import kh.edu.istad.iam.application.usecase.*;
import kh.edu.istad.iam.domain.AssignmentEffect;
import kh.edu.istad.iam.domain.User;
import kh.edu.istad.iam.infrastructure.account.AccountSessionRegistry;
import kh.edu.istad.iam.infrastructure.admin.LoginLockoutService;
import kh.edu.istad.iam.infrastructure.security.LiveAuthorityResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * User administration use cases (create, list, activate/deactivate, lock/unlock, delete, role and direct
 * permission assignment). Each use case is its own bean because several share the same method signature.
 */
public final class UserAdministration {
    private UserAdministration() { }

    /** Shared persistence helpers for the user administration use cases. */
    @Component
    public static class Users {
        final JdbcTemplate jdbc;
        final AccountSessionRegistry sessions;
        final LiveAuthorityResolver authorities;

        public Users(JdbcTemplate jdbc, AccountSessionRegistry sessions, LiveAuthorityResolver authorities) {
            this.jdbc = jdbc;
            this.sessions = sessions;
            this.authorities = authorities;
        }

        long id(UUID userUuid) {
            Long id = jdbc.query("SELECT id FROM users WHERE uuid = ? AND is_deleted = FALSE",
                    rs -> rs.next() ? rs.getLong(1) : null, userUuid);
            if (id == null) throw new IllegalArgumentException("User was not found.");
            return id;
        }

        void notSelf(UUID userUuid, String actor, String what) {
            if (userUuid.toString().equals(actor)) throw new IllegalArgumentException("You cannot " + what + " your own account.");
        }

        void update(UUID userUuid, String set, Instant at, String actor, Object... args) {
            id(userUuid);
            List<Object> all = new ArrayList<>(List.of(args));
            all.add(Timestamp.from(at));
            all.add(actor);
            all.add(userUuid);
            jdbc.update("UPDATE users SET " + set + ", updated_at = ?, updated_by = ?, version = version + 1 WHERE uuid = ?",
                    all.toArray());
            authorities.evict(userUuid);
        }

        /** True when removing ADMIN from this user would leave no active administrator. */
        boolean lastAdmin(long userId) {
            Long others = jdbc.queryForObject("""
                    SELECT count(DISTINCT u.id) FROM user_roles ur JOIN roles r ON r.id = ur.role_id JOIN users u ON u.id = ur.user_id
                    WHERE r.name = 'ADMIN' AND ur.revoked_at IS NULL AND (ur.expires_at IS NULL OR ur.expires_at > now())
                      AND u.is_deleted = FALSE AND u.enabled AND u.account_non_locked AND u.id <> ?""", Long.class, userId);
            Boolean isAdmin = jdbc.queryForObject("""
                    SELECT EXISTS (SELECT 1 FROM user_roles ur JOIN roles r ON r.id = ur.role_id
                                   WHERE ur.user_id = ? AND r.name = 'ADMIN' AND ur.revoked_at IS NULL
                                     AND (ur.expires_at IS NULL OR ur.expires_at > now()))""", Boolean.class, userId);
            return Boolean.TRUE.equals(isAdmin) && (others == null || others == 0);
        }
    }

    @Service
    public static class CreateUser implements CreateUserUseCase {
        private final UserRepository users;
        private final PasswordHasher hasher;
        private final GetUserUseCase get;

        public CreateUser(UserRepository users, PasswordHasher hasher, GetUserUseCase get) {
            this.users = users;
            this.hasher = hasher;
            this.get = get;
        }

        @Override
        @Transactional
        public UserResult execute(CreateUserCommand c) {
            if (users.existsByUsername(c.username())) throw new IllegalArgumentException("Username is already in use.");
            if (users.existsByEmail(c.email())) throw new IllegalArgumentException("Email is already in use.");
            Instant now = Instant.now();
            User user = User.builder()
                    .username(c.username()).passwordHash(hasher.hash(c.password()))
                    .givenName(c.givenName()).familyName(c.familyName()).middleName(c.middleName())
                    .nickname(c.nickname()).gender(c.gender()).birthdate(c.birthdate()).locale(c.locale())
                    .profileUrl(c.profileUrl()).pictureUrl(c.pictureUrl()).websiteUrl(c.websiteUrl())
                    .email(c.email()).phoneNumber(c.phoneNumber()).zoneinfo(c.zoneinfo())
                    .addressStreetAddress(c.addressStreetAddress()).addressLocality(c.addressLocality())
                    .addressRegion(c.addressRegion()).addressPostalCode(c.addressPostalCode())
                    .addressCountry(c.addressCountry())
                    .createdAt(now).createdBy(c.createdBy()).passwordChangedAt(now)
                    .build();
            return get.execute(users.save(user).getUuid());
        }
    }

    @Service
    public static class ListUsers implements ListUsersUseCase {
        private final JdbcTemplate jdbc;

        public ListUsers(JdbcTemplate jdbc) {
            this.jdbc = jdbc;
        }

        @Override
        public PageResult<UserSummary> execute(UserFilterQuery q) {
            StringBuilder where = new StringBuilder(" WHERE is_deleted = ?");
            List<Object> args = new ArrayList<>(List.of(Boolean.TRUE.equals(q.deleted())));
            if (q.search() != null && !q.search().isBlank()) {
                String like = "%" + q.search().trim().toLowerCase(Locale.ROOT) + "%";
                where.append(" AND (lower(username) LIKE ? OR lower(email) LIKE ? OR lower(coalesce(given_name, '') || ' ' || coalesce(family_name, '')) LIKE ?)");
                args.addAll(List.of(like, like, like));
            }
            if (q.enabled() != null) { where.append(" AND enabled = ?"); args.add(q.enabled()); }
            String column = switch (q.sortBy()) {
                case "username" -> "username";
                case "email" -> "email";
                default -> "created_at";
            };
            long total = jdbc.queryForObject("SELECT count(*) FROM users" + where, Long.class, args.toArray());
            args.add(q.size());
            args.add((long) q.page() * q.size());
            List<UserSummary> rows = jdbc.query("SELECT * FROM users" + where + " ORDER BY " + column + " "
                            + q.sortDirection().name() + " LIMIT ? OFFSET ?",
                    (rs, i) -> new UserSummary(rs.getObject("uuid", UUID.class), rs.getString("username"),
                            String.join(" ", Objects.toString(rs.getString("given_name"), ""),
                                    Objects.toString(rs.getString("family_name"), "")).trim(),
                            rs.getString("email"), rs.getBoolean("email_verified"), rs.getBoolean("enabled"),
                            rs.getTimestamp("created_at").toInstant()), args.toArray());
            return new PageResult<>(rows, q.page(), q.size(), total, (int) Math.ceil(total / (double) q.size()));
        }
    }

    @Service
    public static class EnableUser implements EnableUserUseCase {
        private final Users users;
        public EnableUser(Users users) { this.users = users; }

        @Override
        public void execute(UUID userUuid, Instant at, String actor) {
            users.update(userUuid, "enabled = TRUE", at, actor);
        }
    }

    @Service
    public static class DisableUser implements DisableUserUseCase {
        private final Users users;
        public DisableUser(Users users) { this.users = users; }

        @Override
        @Transactional
        public void execute(UUID userUuid, Instant at, String actor) {
            users.notSelf(userUuid, actor, "deactivate");
            if (users.lastAdmin(users.id(userUuid))) throw new IllegalArgumentException("You cannot deactivate the last administrator.");
            users.update(userUuid, "enabled = FALSE", at, actor);
            users.sessions.revokeAll(userUuid, null, "account_disabled_by_admin");
        }
    }

    @Service
    public static class LockUser implements LockUserUseCase {
        private final Users users;
        public LockUser(Users users) { this.users = users; }

        @Override
        @Transactional
        public void execute(UUID userUuid, Instant at, String actor) {
            users.notSelf(userUuid, actor, "lock");
            if (users.lastAdmin(users.id(userUuid))) throw new IllegalArgumentException("You cannot lock the last administrator.");
            users.update(userUuid, "account_non_locked = FALSE, lock_expires_at = NULL", at, actor);
            users.sessions.revokeAll(userUuid, null, "account_locked_by_admin");
        }
    }

    @Service
    public static class UnlockUser implements UnlockUserUseCase {
        private final Users users;
        private final LoginLockoutService lockout;
        public UnlockUser(Users users, LoginLockoutService lockout) { this.users = users; this.lockout = lockout; }

        @Override
        public void execute(UUID userUuid, Instant at, String actor) {
            users.update(userUuid, "account_non_locked = TRUE", at, actor);
            lockout.unlock(userUuid);
        }
    }

    @Service
    public static class DeleteUser implements DeleteUserUseCase {
        private final Users users;
        public DeleteUser(Users users) { this.users = users; }

        @Override
        @Transactional
        public void execute(UUID userUuid, Instant at, String actor) {
            users.notSelf(userUuid, actor, "delete");
            long id = users.id(userUuid);
            if (users.lastAdmin(id)) throw new IllegalArgumentException("You cannot delete the last administrator.");
            users.sessions.revokeAll(userUuid, null, "account_deleted");
            Timestamp now = Timestamp.from(at);
            users.jdbc.update("UPDATE user_roles SET revoked_at = ?, revoked_by = ?, reason = 'User deleted' WHERE user_id = ? AND revoked_at IS NULL",
                    now, actor, id);
            users.jdbc.update("UPDATE user_perms SET revoked_at = ?, revoked_by = ?, reason = 'User deleted' WHERE user_id = ? AND revoked_at IS NULL",
                    now, actor, id);
            users.update(userUuid, "is_deleted = TRUE, deleted_at = ?, deleted_by = ?, enabled = FALSE", at, actor, now, actor);
        }
    }

    @Service
    public static class AssignRoles implements AssignRolesToUserUseCase {
        private final Users users;
        public AssignRoles(Users users) { this.users = users; }

        @Override
        @Transactional
        public List<RoleAssignmentResult> execute(AssignRolesToUserCommand c) {
            long userId = users.id(c.userUuid());
            if (c.roleUuids().isEmpty()) throw new IllegalArgumentException("Select at least one role.");
            Timestamp expires = c.expiresAt() == null ? null : Timestamp.from(c.expiresAt());
            List<RoleAssignmentResult> results = new ArrayList<>();
            for (UUID roleUuid : c.roleUuids()) {
                List<Map<String, Object>> role = users.jdbc.queryForList(
                        "SELECT id, name, is_active FROM roles WHERE uuid = ? AND is_deleted = FALSE", roleUuid);
                if (role.isEmpty()) throw new IllegalArgumentException("Role was not found: " + roleUuid);
                if (!(Boolean) role.getFirst().get("is_active")) {
                    throw new IllegalArgumentException("Role " + role.getFirst().get("name") + " is inactive.");
                }
                long roleId = ((Number) role.getFirst().get("id")).longValue();
                List<Map<String, Object>> existing = users.jdbc.queryForList("""
                        SELECT * FROM user_roles WHERE user_id = ? AND role_id = ? AND revoked_at IS NULL
                          AND (expires_at IS NULL OR expires_at > now())""", userId, roleId);
                Map<String, Object> row = existing.isEmpty() ? users.jdbc.queryForMap("""
                        INSERT INTO user_roles (user_id, role_id, assigned_at, assigned_by, expires_at, reason)
                        VALUES (?, ?, now(), ?, ?, ?) RETURNING *""", userId, roleId, c.assignedBy(), expires, c.reason())
                        : existing.getFirst();
                results.add(new RoleAssignmentResult(((Number) row.get("id")).longValue(), roleUuid,
                        (String) role.getFirst().get("name"), ts(row.get("assigned_at")), (String) row.get("assigned_by"),
                        ts(row.get("expires_at")), null, null, (String) row.get("reason")));
            }
            users.authorities.evict(c.userUuid());
            return results;
        }
    }

    @Service
    public static class RevokeRole implements RevokeRoleFromUserUseCase {
        private final Users users;
        public RevokeRole(Users users) { this.users = users; }

        @Override
        @Transactional
        public void execute(UUID userUuid, UUID roleUuid, String reason, String revokedBy, Instant revokedAt) {
            long userId = users.id(userUuid);
            String role = users.jdbc.query("SELECT name FROM roles WHERE uuid = ?", rs -> rs.next() ? rs.getString(1) : null, roleUuid);
            if ("ADMIN".equals(role) && users.lastAdmin(userId)) {
                throw new IllegalArgumentException("You cannot remove the ADMIN role from the last administrator.");
            }
            int updated = users.jdbc.update("""
                    UPDATE user_roles SET revoked_at = ?, revoked_by = ?, reason = coalesce(?, reason)
                    WHERE user_id = ? AND role_id = (SELECT id FROM roles WHERE uuid = ?) AND revoked_at IS NULL""",
                    Timestamp.from(revokedAt), revokedBy, reason, userId, roleUuid);
            if (updated == 0) throw new IllegalArgumentException("This role is not assigned to the user.");
            users.authorities.evict(userUuid);
        }
    }

    @Service
    public static class GrantPermissions implements GrantPermissionsToUserUseCase {
        private final Users users;
        public GrantPermissions(Users users) { this.users = users; }

        @Override
        @Transactional
        public List<PermissionGrantResult> execute(GrantPermissionsToUserCommand c) {
            long userId = users.id(c.userUuid());
            if (c.permissionUuids().isEmpty()) throw new IllegalArgumentException("Select at least one permission.");
            AssignmentEffect effect = c.effect() == null ? AssignmentEffect.GRANT : c.effect();
            Timestamp expires = c.expiresAt() == null ? null : Timestamp.from(c.expiresAt());
            List<PermissionGrantResult> results = new ArrayList<>();
            for (UUID permissionUuid : c.permissionUuids()) {
                List<Map<String, Object>> permission = users.jdbc.queryForList(
                        "SELECT id, name FROM permissions WHERE uuid = ? AND is_deleted = FALSE", permissionUuid);
                if (permission.isEmpty()) throw new IllegalArgumentException("Permission was not found: " + permissionUuid);
                long permissionId = ((Number) permission.getFirst().get("id")).longValue();
                // One active direct assignment per permission: replace any existing one (e.g. GRANT -> DENY).
                users.jdbc.update("""
                        UPDATE user_perms SET revoked_at = now(), revoked_by = ?, reason = 'Replaced'
                        WHERE user_id = ? AND permission_id = ? AND revoked_at IS NULL""", c.assignedBy(), userId, permissionId);
                Map<String, Object> row = users.jdbc.queryForMap("""
                        INSERT INTO user_perms (user_id, permission_id, effect, assigned_at, assigned_by, expires_at, reason)
                        VALUES (?, ?, ?, now(), ?, ?, ?) RETURNING *""",
                        userId, permissionId, effect.name(), c.assignedBy(), expires, c.reason());
                results.add(new PermissionGrantResult(((Number) row.get("id")).longValue(), permissionUuid,
                        (String) permission.getFirst().get("name"), effect, ts(row.get("assigned_at")),
                        c.assignedBy(), ts(row.get("expires_at")), null, null, c.reason()));
            }
            users.authorities.evict(c.userUuid());
            return results;
        }
    }

    @Service
    public static class RevokePermission implements RevokePermissionFromUserUseCase {
        private final Users users;
        public RevokePermission(Users users) { this.users = users; }

        @Override
        public void execute(UUID userUuid, UUID permissionUuid, String reason, String revokedBy, Instant revokedAt) {
            long userId = users.id(userUuid);
            int updated = users.jdbc.update("""
                    UPDATE user_perms SET revoked_at = ?, revoked_by = ?, reason = coalesce(?, reason)
                    WHERE user_id = ? AND permission_id = (SELECT id FROM permissions WHERE uuid = ?) AND revoked_at IS NULL""",
                    Timestamp.from(revokedAt), revokedBy, reason, userId, permissionUuid);
            if (updated == 0) throw new IllegalArgumentException("This permission is not assigned to the user.");
            users.authorities.evict(userUuid);
        }
    }

    static Instant ts(Object value) {
        if (value instanceof Timestamp t) return t.toInstant();
        if (value instanceof java.time.OffsetDateTime o) return o.toInstant();
        return null;
    }
}
