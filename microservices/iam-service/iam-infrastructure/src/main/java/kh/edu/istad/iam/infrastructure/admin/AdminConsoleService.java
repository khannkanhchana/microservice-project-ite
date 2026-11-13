package kh.edu.istad.iam.infrastructure.admin;

import kh.edu.istad.iam.application.account.AccountActivityType;
import kh.edu.istad.iam.application.account.AccountSelfServiceUseCase;
import kh.edu.istad.iam.application.admin.AdminConsoleUseCase;
import kh.edu.istad.iam.application.admin.AdminModels.*;
import kh.edu.istad.iam.application.result.PageResult;
import kh.edu.istad.iam.infrastructure.account.AccountAuditLog;
import kh.edu.istad.iam.infrastructure.account.AccountSessionRegistry;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** Read models (SQL + Redis) and account-security actions of the administration console. */
@Service
public class AdminConsoleService implements AdminConsoleUseCase {
    private final JdbcTemplate jdbc;
    private final AccountSessionRegistry sessions;
    private final AccountAuditLog accountAudit;
    private final AuditLogWriter auditLog;
    private final LoginLockoutService lockout;
    private final AccountSelfServiceUseCase account;

    public AdminConsoleService(JdbcTemplate jdbc, AccountSessionRegistry sessions, AccountAuditLog accountAudit,
                               AuditLogWriter auditLog, LoginLockoutService lockout, AccountSelfServiceUseCase account) {
        this.jdbc = jdbc;
        this.sessions = sessions;
        this.accountAudit = accountAudit;
        this.auditLog = auditLog;
        this.lockout = lockout;
        this.account = account;
    }

    // ---- Overview ---------------------------------------------------------------------------------------------

    @Override
    public Overview overview() {
        Map<String, Object> users = jdbc.queryForMap("""
                SELECT count(*) AS total,
                       count(*) FILTER (WHERE enabled AND account_non_locked) AS active,
                       count(*) FILTER (WHERE NOT enabled) AS disabled,
                       count(*) FILTER (WHERE NOT account_non_locked) AS locked,
                       count(*) FILTER (WHERE created_at > now() - interval '7 days') AS recent
                FROM users WHERE is_deleted = FALSE""");
        Map<String, Object> logins = jdbc.queryForMap("""
                SELECT count(*) FILTER (WHERE success) AS ok, count(*) FILTER (WHERE NOT success) AS failed
                FROM login_attempts WHERE attempted_at > now() - interval '24 hours'""");
        long roles = count("SELECT count(*) FROM roles WHERE is_deleted = FALSE");
        long permissions = count("SELECT count(*) FROM permissions WHERE is_deleted = FALSE");
        long clients = count("SELECT count(*) FROM oauth2_registered_client");
        List<DailyLogins> daily = jdbc.query("""
                SELECT to_char(d.day, 'YYYY-MM-DD') AS day,
                       count(la.id) FILTER (WHERE la.success) AS ok,
                       count(la.id) FILTER (WHERE NOT la.success) AS failed
                FROM generate_series(date_trunc('day', now()) - interval '6 days', date_trunc('day', now()),
                                     interval '1 day') AS d(day)
                LEFT JOIN login_attempts la ON la.attempted_at >= d.day AND la.attempted_at < d.day + interval '1 day'
                GROUP BY d.day ORDER BY d.day""",
                (rs, i) -> new DailyLogins(rs.getString("day"), rs.getLong("ok"), rs.getLong("failed")));
        List<AuditLogEntry> recent = jdbc.query(
                "SELECT * FROM audit_logs ORDER BY occurred_at DESC LIMIT 12", AUDIT_ROW);
        long signedIn = sessions.activeCounts()[0];
        return new Overview(num(users.get("total")), num(users.get("active")), num(users.get("disabled")),
                num(users.get("locked")), signedIn, num(users.get("recent")), roles, permissions, clients,
                num(logins.get("ok")), num(logins.get("failed")), suspiciousActivity(7).size(), daily, recent);
    }

    // ---- Users ------------------------------------------------------------------------------------------------

    @Override
    public PageResult<AdminUserRow> users(UserQuery query) {
        Sql where = new Sql("u.is_deleted = FALSE");
        if (notBlank(query.search())) {
            String like = like(query.search());
            where.and("(lower(u.username) LIKE ? OR lower(u.email) LIKE ? OR lower(coalesce(u.given_name, '') || ' ' || coalesce(u.family_name, '')) LIKE ?)",
                    like, like, like);
        }
        switch (lower(query.status())) {
            case "active" -> where.and("u.enabled AND u.account_non_locked");
            case "disabled" -> where.and("NOT u.enabled");
            case "locked" -> where.and("NOT u.account_non_locked");
            default -> { }
        }
        if (query.roleUuid() != null) {
            where.and("""
                    EXISTS (SELECT 1 FROM user_roles ur JOIN roles r ON r.id = ur.role_id
                            WHERE ur.user_id = u.id AND r.uuid = ? AND ur.revoked_at IS NULL
                              AND (ur.expires_at IS NULL OR ur.expires_at > now()))""", query.roleUuid());
        }
        int size = Math.clamp(query.size(), 1, 100);
        int page = Math.max(query.page(), 0);
        long total = jdbc.queryForObject("SELECT count(*) FROM users u WHERE " + where.sql, Long.class, where.args());
        List<AdminUserRow> rows = jdbc.query("""
                SELECT u.uuid, u.username, u.email, u.given_name, u.family_name, u.picture_url, u.enabled,
                       u.account_non_locked, u.email_verified, u.failed_login_attempts, u.last_login_at, u.created_at,
                       (SELECT string_agg(r.name, ',' ORDER BY r.name) FROM user_roles ur JOIN roles r ON r.id = ur.role_id
                        WHERE ur.user_id = u.id AND ur.revoked_at IS NULL AND r.is_deleted = FALSE
                          AND (ur.expires_at IS NULL OR ur.expires_at > now())) AS roles
                FROM users u WHERE\s""" + where.sql + " ORDER BY u.created_at DESC LIMIT ? OFFSET ?",
                (rs, i) -> new AdminUserRow(rs.getObject("uuid", UUID.class), rs.getString("username"),
                        name(rs.getString("given_name"), rs.getString("family_name")), rs.getString("email"),
                        rs.getString("picture_url"), rs.getBoolean("enabled"), !rs.getBoolean("account_non_locked"),
                        rs.getBoolean("email_verified"), split(rs.getString("roles")),
                        rs.getInt("failed_login_attempts"), instant(rs, "last_login_at"), instant(rs, "created_at")),
                where.args(size, (long) page * size));
        return page(rows, page, size, total);
    }

    @Override
    public List<RolePermission> rolePermissions(UUID roleUuid) {
        return jdbc.query("""
                SELECT p.uuid, p.name, p.resource, p.action, p.description, rp.assigned_at, rp.assigned_by
                FROM role_perms rp JOIN roles r ON r.id = rp.role_id JOIN permissions p ON p.id = rp.permission_id
                WHERE r.uuid = ? AND rp.revoked_at IS NULL AND p.is_deleted = FALSE
                  AND (rp.expires_at IS NULL OR rp.expires_at > now())
                ORDER BY p.resource, p.action""",
                (rs, i) -> new RolePermission(rs.getObject("uuid", UUID.class), rs.getString("name"),
                        rs.getString("resource"), rs.getString("action"), rs.getString("description"),
                        instant(rs, "assigned_at"), rs.getString("assigned_by")), roleUuid);
    }

    @Override
    public AccountSecurity accountSecurity(UUID userUuid) {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT u.enabled, u.account_non_locked, u.lock_expires_at, u.failed_login_attempts,
                       u.password_changed_at, u.last_login_at,
                       (SELECT count(*) FROM login_attempts la WHERE la.user_uuid = u.uuid AND NOT la.success
                          AND la.attempted_at > now() - interval '24 hours') AS failed24,
                       (SELECT string_agg(lower(i.provider), ',' ORDER BY i.provider) FROM identities i
                        WHERE i.user_id = u.id AND i.is_deleted = FALSE) AS providers
                FROM users u WHERE u.uuid = ? AND u.is_deleted = FALSE""", userUuid);
        if (rows.isEmpty()) throw new IllegalArgumentException("User was not found.");
        Map<String, Object> r = rows.getFirst();
        return new AccountSecurity(userUuid, (Boolean) r.get("enabled"), !(Boolean) r.get("account_non_locked"),
                ts(r.get("lock_expires_at")), ((Number) r.get("failed_login_attempts")).intValue(),
                account.hasPassword(userUuid), ts(r.get("password_changed_at")), ts(r.get("last_login_at")),
                sessions.list(userUuid).size(), num(r.get("failed24")), split((String) r.get("providers")));
    }

    @Override
    public void unlockAccount(UUID userUuid, String actor) {
        requireUser(userUuid);
        lockout.unlock(userUuid);
        accountAudit.activity(userUuid, AccountActivityType.ACCOUNT_UNLOCKED_BY_ADMIN, "Unlocked by an administrator", actor);
    }

    @Override
    public void sendPasswordResetLink(UUID userUuid, String resetPageUrl, String actor) {
        String email = jdbc.query("SELECT email FROM users WHERE uuid = ? AND is_deleted = FALSE",
                rs -> rs.next() ? rs.getString(1) : null, userUuid);
        if (email == null) throw new IllegalArgumentException("User was not found.");
        account.requestPasswordReset(email, resetPageUrl);
        accountAudit.activity(userUuid, AccountActivityType.PASSWORD_RESET_LINK_SENT,
                "Password reset link sent by an administrator", actor);
    }

    // ---- Sessions ---------------------------------------------------------------------------------------------

    @Override
    public List<AdminSession> userSessions(UUID userUuid) {
        String username = requireUser(userUuid);
        return sessions.list(userUuid).stream().map(s -> new AdminSession(s.id(), userUuid, username,
                s.loginMethod(), s.browser(), s.os(), s.deviceType(), s.ipAddress(), s.location(), s.createdAt(),
                s.lastActivityAt())).toList();
    }

    @Override
    public void revokeUserSession(UUID userUuid, UUID sessionId, String actor) {
        requireUser(userUuid);
        if (!sessions.revoke(userUuid, sessionId, "revoked_by_admin")) {
            throw new IllegalArgumentException("Session was not found or has already ended.");
        }
        accountAudit.activity(userUuid, AccountActivityType.SESSION_REVOKED, "Ended by an administrator", actor);
    }

    @Override
    public int revokeAllUserSessions(UUID userUuid, String actor) {
        requireUser(userUuid);
        int revoked = sessions.revokeAll(userUuid, null, "revoked_by_admin");
        if (revoked > 0) {
            accountAudit.activity(userUuid, AccountActivityType.OTHER_SESSIONS_REVOKED,
                    revoked + " session(s) ended by an administrator", actor);
        }
        return revoked;
    }

    // ---- Login monitoring -------------------------------------------------------------------------------------

    @Override
    public PageResult<AdminLoginAttempt> loginHistory(LoginHistoryQuery q) {
        Sql where = new Sql("TRUE");
        if (q.userUuid() != null) where.and("la.user_uuid = ?", q.userUuid());
        if (notBlank(q.search())) {
            String like = like(q.search());
            where.and("(lower(coalesce(la.identifier, '')) LIKE ? OR lower(coalesce(u.username, '')) LIKE ? OR coalesce(la.ip_address, '') LIKE ?)",
                    like, like, like);
        }
        switch (lower(q.status())) {
            case "success" -> where.and("la.success");
            case "failed" -> where.and("NOT la.success");
            default -> { }
        }
        if (notBlank(q.method())) where.and("la.method = ?", q.method().toLowerCase(Locale.ROOT));
        if (notBlank(q.ipAddress())) where.and("la.ip_address = ?", q.ipAddress().trim());
        if (q.from() != null) where.and("la.attempted_at >= ?", Timestamp.from(q.from()));
        if (q.to() != null) where.and("la.attempted_at < ?", Timestamp.from(q.to()));
        int size = Math.clamp(q.size(), 1, 100);
        int page = Math.max(q.page(), 0);
        String from = " FROM login_attempts la LEFT JOIN users u ON u.uuid = la.user_uuid WHERE ";
        long total = jdbc.queryForObject("SELECT count(*)" + from + where.sql, Long.class, where.args());
        List<AdminLoginAttempt> rows = jdbc.query("SELECT la.*, u.username" + from + where.sql
                        + " ORDER BY la.attempted_at DESC LIMIT ? OFFSET ?",
                (rs, i) -> new AdminLoginAttempt(rs.getObject("id", UUID.class), rs.getObject("user_uuid", UUID.class),
                        rs.getString("username"), rs.getString("identifier"), rs.getBoolean("success"),
                        rs.getString("method"), rs.getString("failure_reason"), rs.getString("ip_address"),
                        rs.getString("browser"), rs.getString("os"), rs.getString("device_type"),
                        rs.getString("location"), instant(rs, "attempted_at")),
                where.args(size, (long) page * size));
        return page(rows, page, size, total);
    }

    @Override
    public List<SuspiciousActivity> suspiciousActivity(int days) {
        int window = Math.clamp(days, 1, 90);
        List<SuspiciousActivity> found = new ArrayList<>();
        // Many failed sign-ins against one account.
        found.addAll(jdbc.query("""
                SELECT lower(identifier) AS subject, (array_agg(user_uuid) FILTER (WHERE user_uuid IS NOT NULL))[1] AS user_uuid,
                       (array_agg(ip_address ORDER BY attempted_at DESC))[1] AS ip, count(*) AS attempts,
                       count(DISTINCT ip_address) AS sources, min(attempted_at) AS first_seen, max(attempted_at) AS last_seen
                FROM login_attempts
                WHERE NOT success AND identifier IS NOT NULL AND attempted_at > now() - make_interval(days => ?)
                GROUP BY lower(identifier) HAVING count(*) >= 5""",
                (rs, i) -> new SuspiciousActivity("BRUTE_FORCE", rs.getLong("attempts") >= 10 ? "high" : "medium",
                        rs.getString("subject"), rs.getObject("user_uuid", UUID.class), rs.getString("ip"),
                        rs.getLong("attempts"), 1, instant(rs, "first_seen"), instant(rs, "last_seen")), window));
        // One source trying many accounts.
        found.addAll(jdbc.query("""
                SELECT ip_address, count(*) AS attempts, count(DISTINCT lower(identifier)) AS accounts,
                       min(attempted_at) AS first_seen, max(attempted_at) AS last_seen
                FROM login_attempts
                WHERE NOT success AND ip_address IS NOT NULL AND attempted_at > now() - make_interval(days => ?)
                GROUP BY ip_address HAVING count(DISTINCT lower(identifier)) >= 3""",
                (rs, i) -> new SuspiciousActivity("PASSWORD_SPRAY", rs.getLong("accounts") >= 10 ? "high" : "medium",
                        rs.getString("ip_address"), null, rs.getString("ip_address"), rs.getLong("attempts"),
                        rs.getLong("accounts"), instant(rs, "first_seen"), instant(rs, "last_seen")), window));
        // A successful sign-in right after a run of failures (possible guessed password).
        found.addAll(jdbc.query("""
                SELECT s.user_uuid, coalesce(u.username, s.identifier) AS subject, s.ip_address, s.attempted_at,
                       f.failures, f.first_failure
                FROM login_attempts s
                LEFT JOIN users u ON u.uuid = s.user_uuid
                CROSS JOIN LATERAL (
                    SELECT count(*) AS failures, min(x.attempted_at) AS first_failure FROM login_attempts x
                    WHERE x.user_uuid = s.user_uuid AND NOT x.success
                      AND x.attempted_at BETWEEN s.attempted_at - interval '1 hour' AND s.attempted_at) f
                WHERE s.success AND s.user_uuid IS NOT NULL AND s.attempted_at > now() - make_interval(days => ?)
                  AND f.failures >= 3""",
                (rs, i) -> new SuspiciousActivity("SUCCESS_AFTER_FAILURES", "high", rs.getString("subject"),
                        rs.getObject("user_uuid", UUID.class), rs.getString("ip_address"), rs.getLong("failures"), 1,
                        instant(rs, "first_failure"), instant(rs, "attempted_at")), window));
        // Attempts against locked or disabled accounts.
        found.addAll(jdbc.query("""
                SELECT la.user_uuid, coalesce(u.username, min(la.identifier)) AS subject,
                       (array_agg(la.ip_address ORDER BY la.attempted_at DESC))[1] AS ip, count(*) AS attempts,
                       min(la.attempted_at) AS first_seen, max(la.attempted_at) AS last_seen
                FROM login_attempts la LEFT JOIN users u ON u.uuid = la.user_uuid
                WHERE NOT la.success AND la.failure_reason IN ('account_locked', 'account_disabled')
                  AND la.attempted_at > now() - make_interval(days => ?)
                GROUP BY la.user_uuid, u.username""",
                (rs, i) -> new SuspiciousActivity("LOCKED_ACCOUNT_ATTEMPTS", "low", rs.getString("subject"),
                        rs.getObject("user_uuid", UUID.class), rs.getString("ip"), rs.getLong("attempts"), 1,
                        instant(rs, "first_seen"), instant(rs, "last_seen")), window));
        found.sort(Comparator.comparing(SuspiciousActivity::lastSeen, Comparator.nullsLast(Comparator.reverseOrder())));
        return found;
    }

    // ---- Audit log --------------------------------------------------------------------------------------------

    @Override
    public PageResult<AuditLogEntry> auditLogs(AuditQuery q) {
        Sql where = new Sql("TRUE");
        if (notBlank(q.category())) where.and("category = ?", q.category().toUpperCase(Locale.ROOT));
        if (notBlank(q.action())) where.and("action = ?", q.action().toUpperCase(Locale.ROOT));
        if (notBlank(q.outcome())) where.and("outcome = ?", q.outcome().toUpperCase(Locale.ROOT));
        if (notBlank(q.actor())) where.and("lower(coalesce(actor, '')) LIKE ?", like(q.actor()));
        if (notBlank(q.target())) where.and("lower(coalesce(target_id, '')) LIKE ?", like(q.target()));
        if (notBlank(q.search())) {
            String like = like(q.search());
            where.and("(lower(coalesce(description, '')) LIKE ? OR lower(coalesce(path, '')) LIKE ? OR lower(coalesce(details, '')) LIKE ?)",
                    like, like, like);
        }
        if (q.from() != null) where.and("occurred_at >= ?", Timestamp.from(q.from()));
        if (q.to() != null) where.and("occurred_at < ?", Timestamp.from(q.to()));
        int size = Math.clamp(q.size(), 1, 100);
        int page = Math.max(q.page(), 0);
        long total = jdbc.queryForObject("SELECT count(*) FROM audit_logs WHERE " + where.sql, Long.class, where.args());
        List<AuditLogEntry> rows = jdbc.query("SELECT * FROM audit_logs WHERE " + where.sql
                + " ORDER BY occurred_at DESC LIMIT ? OFFSET ?", AUDIT_ROW, where.args(size, (long) page * size));
        return page(rows, page, size, total);
    }

    @Override
    public void audit(AuditEvent event) {
        auditLog.write(event);
    }

    // ---- helpers ----------------------------------------------------------------------------------------------

    private static final RowMapper<AuditLogEntry> AUDIT_ROW = (rs, i) -> new AuditLogEntry(
            rs.getObject("id", UUID.class), instant(rs, "occurred_at"), rs.getString("category"), rs.getString("action"),
            rs.getString("outcome"), rs.getString("actor"), rs.getObject("actor_uuid", UUID.class),
            rs.getString("target_type"), rs.getString("target_id"), rs.getString("description"),
            rs.getString("details"), rs.getString("ip_address"), rs.getString("http_method"), rs.getString("path"),
            (Integer) rs.getObject("status"));

    private String requireUser(UUID userUuid) {
        String username = jdbc.query("SELECT username FROM users WHERE uuid = ? AND is_deleted = FALSE",
                rs -> rs.next() ? rs.getString(1) : null, userUuid);
        if (username == null) throw new IllegalArgumentException("User was not found.");
        return username;
    }

    private long count(String sql) {
        Long value = jdbc.queryForObject(sql, Long.class);
        return value == null ? 0 : value;
    }

    private static <T> PageResult<T> page(List<T> rows, int page, int size, long total) {
        return new PageResult<>(rows, page, size, total, (int) Math.ceil(total / (double) size));
    }

    private static long num(Object value) {
        return value == null ? 0 : ((Number) value).longValue();
    }

    private static Instant ts(Object value) {
        if (value instanceof Timestamp t) return t.toInstant();
        if (value instanceof java.time.OffsetDateTime o) return o.toInstant();
        return null;
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }

    private static String name(String given, String family) {
        return Arrays.stream(new String[] { given, family }).filter(v -> v != null && !v.isBlank())
                .collect(Collectors.joining(" "));
    }

    private static List<String> split(String csv) {
        return csv == null || csv.isBlank() ? List.of() : List.of(csv.split(","));
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private static String lower(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String like(String value) {
        return "%" + value.trim().toLowerCase(Locale.ROOT).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
    }

    /** Small WHERE-clause builder with positional arguments. */
    private static final class Sql {
        private String sql;
        private final List<Object> args = new ArrayList<>();

        Sql(String initial) { this.sql = initial; }

        void and(String condition, Object... values) {
            sql += " AND " + condition;
            args.addAll(Arrays.asList(values));
        }

        Object[] args(Object... extra) {
            List<Object> all = new ArrayList<>(args);
            all.addAll(Arrays.asList(extra));
            return all.toArray();
        }
    }
}
