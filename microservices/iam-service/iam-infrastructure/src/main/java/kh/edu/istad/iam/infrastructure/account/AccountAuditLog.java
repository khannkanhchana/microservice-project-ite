package kh.edu.istad.iam.infrastructure.account;

import kh.edu.istad.iam.application.admin.AdminModels.AuditEvent;
import kh.edu.istad.iam.infrastructure.admin.AuditLogWriter;

import kh.edu.istad.iam.application.account.AccountActivityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Append-only login history and security activity records. Failures to audit never break the caller. */
@Component
public class AccountAuditLog {
    private static final Logger log = LoggerFactory.getLogger(AccountAuditLog.class);

    private final JdbcTemplate jdbc;
    private final ClientInfoResolver clients;
    private final AuditLogWriter auditLog;

    public AccountAuditLog(JdbcTemplate jdbc, ClientInfoResolver clients, AuditLogWriter auditLog) {
        this.jdbc = jdbc;
        this.clients = clients;
        this.auditLog = auditLog;
    }

    public void loginAttempt(UUID userUuid, String identifier, boolean success, String method, String failureReason,
                             ClientInfo client) {
        try {
            jdbc.update("""
                    INSERT INTO login_attempts (id, user_uuid, identifier, success, method, failure_reason, ip_address,
                        user_agent, browser, os, device_type, location, attempted_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
                    UUID.randomUUID(), userUuid, truncate(identifier, 255), success, method,
                    truncate(failureReason, 255), client.ipAddress(), client.userAgent(), client.browser(),
                    client.os(), client.deviceType(), client.location(), Timestamp.from(Instant.now()));
        } catch (RuntimeException exception) {
            log.warn("Could not record login attempt for {}", identifier, exception);
        }
        auditLog.write(new AuditEvent(AuditLogWriter.AUTHENTICATION, success ? "LOGIN_SUCCESS" : "LOGIN_FAILURE",
                success ? "SUCCESS" : "FAILURE", identifier, userUuid, "USER",
                userUuid == null ? identifier : userUuid.toString(),
                (success ? "Signed in with " : "Failed sign-in with ") + method
                        + (failureReason == null ? "" : " (" + failureReason + ")"),
                null, client.ipAddress(), client.userAgent(), null, null, null));
    }

    public void activity(UUID userUuid, AccountActivityType type, String description, String actor) {
        activity(userUuid, type, description, actor, clients.current());
    }

    public void activity(UUID userUuid, AccountActivityType type, String description, String actor, ClientInfo client) {
        try {
            jdbc.update("""
                    INSERT INTO account_activities (id, user_uuid, activity_type, description, actor, ip_address,
                        user_agent, browser, os, device_type, occurred_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
                    UUID.randomUUID(), userUuid, type.name(), truncate(description, 500), truncate(actor, 255),
                    client.ipAddress(), client.userAgent(), client.browser(), client.os(), client.deviceType(),
                    Timestamp.from(Instant.now()));
        } catch (RuntimeException exception) {
            log.warn("Could not record {} activity for {}", type, userUuid, exception);
        }
        auditLog.write(new AuditEvent(AuditLogWriter.ACCOUNT, type.name(), "SUCCESS", actor, null, "USER",
                userUuid == null ? null : userUuid.toString(), description, null, client.ipAddress(),
                client.userAgent(), null, null, null));
    }

    /** Resolves the account a login identifier (username or email) refers to, if any. */
    public UUID findUserUuid(String usernameOrEmail) {
        if (usernameOrEmail == null || usernameOrEmail.isBlank()) return null;
        List<UUID> matches = jdbc.queryForList(
                "SELECT uuid FROM users WHERE lower(username) = ? OR lower(email) = ? LIMIT 1", UUID.class,
                usernameOrEmail.trim().toLowerCase(Locale.ROOT), usernameOrEmail.trim().toLowerCase(Locale.ROOT));
        return matches.isEmpty() ? null : matches.getFirst();
    }

    private static String truncate(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }
}
