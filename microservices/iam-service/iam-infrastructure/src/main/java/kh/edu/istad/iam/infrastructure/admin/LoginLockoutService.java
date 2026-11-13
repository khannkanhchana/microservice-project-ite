package kh.edu.istad.iam.infrastructure.admin;

import kh.edu.istad.iam.application.admin.AdminModels.AuditEvent;
import kh.edu.istad.iam.application.admin.AdminModels.AuthenticationPolicy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Enforces the authentication policy's lockout: after {@code maxFailedAttempts} consecutive failed password
 * sign-ins the account is locked, for {@code lockoutMinutes} (0 = until an administrator unlocks it).
 * A successful sign-in resets the counter and records the last login time.
 */
@Component
public class LoginLockoutService {
    private final JdbcTemplate jdbc;
    private final SystemSettingsService settings;
    private final AuditLogWriter audit;

    public LoginLockoutService(JdbcTemplate jdbc, SystemSettingsService settings, AuditLogWriter audit) {
        this.jdbc = jdbc;
        this.settings = settings;
        this.audit = audit;
    }

    /** Lifts a timed lock whose period has passed, before the account is loaded for authentication. */
    public void releaseExpiredLock(String usernameOrEmail) {
        if (usernameOrEmail == null || usernameOrEmail.isBlank()) return;
        String identifier = usernameOrEmail.trim().toLowerCase(Locale.ROOT);
        jdbc.update("""
                UPDATE users SET account_non_locked = TRUE, failed_login_attempts = 0, lock_expires_at = NULL
                WHERE (lower(username) = ? OR lower(email) = ?) AND account_non_locked = FALSE
                  AND lock_expires_at IS NOT NULL AND lock_expires_at <= now()""", identifier, identifier);
    }

    /** Counts a wrong password; locks the account when the policy limit is reached. Returns true when locked now. */
    public boolean recordFailure(UUID userUuid) {
        if (userUuid == null) return false;
        AuthenticationPolicy policy = settings.get().authentication();
        List<Map<String, Object>> rows = jdbc.queryForList("""
                UPDATE users SET failed_login_attempts = failed_login_attempts + 1
                WHERE uuid = ? AND account_non_locked = TRUE AND is_deleted = FALSE
                RETURNING failed_login_attempts, username""", userUuid);
        if (rows.isEmpty() || policy.maxFailedAttempts() <= 0) return false;
        int attempts = ((Number) rows.getFirst().get("failed_login_attempts")).intValue();
        if (attempts < policy.maxFailedAttempts()) return false;

        Timestamp until = policy.lockoutMinutes() <= 0 ? null
                : Timestamp.from(Instant.now().plus(Duration.ofMinutes(policy.lockoutMinutes())));
        jdbc.update("UPDATE users SET account_non_locked = FALSE, lock_expires_at = ? WHERE uuid = ?", until, userUuid);
        audit.write(new AuditEvent("SECURITY", "ACCOUNT_LOCKED", "SUCCESS", "system", null, "USER",
                userUuid.toString(), "Locked " + rows.getFirst().get("username") + " after " + attempts
                + " failed sign-in attempts" + (until == null ? " until an administrator unlocks it"
                : " for " + policy.lockoutMinutes() + " minutes"), null, null, null, null, null, null));
        return true;
    }

    public void recordSuccess(UUID userUuid) {
        if (userUuid == null) return;
        jdbc.update("UPDATE users SET failed_login_attempts = 0, last_login_at = ? WHERE uuid = ?",
                Timestamp.from(Instant.now()), userUuid);
    }

    /** Administrator unlock: clears the lock and the failure counter. */
    public void unlock(UUID userUuid) {
        jdbc.update("UPDATE users SET account_non_locked = TRUE, failed_login_attempts = 0, lock_expires_at = NULL WHERE uuid = ?",
                userUuid);
    }

    /** True when the password is older than the policy's maximum age. */
    public boolean passwordExpired(UUID userUuid) {
        int maxAgeDays = settings.get().password().maxAgeDays();
        if (maxAgeDays <= 0 || userUuid == null) return false;
        Timestamp changed = jdbc.query("SELECT password_changed_at FROM users WHERE uuid = ?",
                rs -> rs.next() ? rs.getTimestamp(1) : null, userUuid);
        return changed != null && changed.toInstant().plus(Duration.ofDays(maxAgeDays)).isBefore(Instant.now());
    }
}
