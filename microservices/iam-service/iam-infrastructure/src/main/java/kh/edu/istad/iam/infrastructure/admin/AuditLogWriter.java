package kh.edu.istad.iam.infrastructure.admin;

import kh.edu.istad.iam.application.admin.AdminModels.AuditEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

/**
 * Appends to the system audit log (audit_logs). Used by the admin API audit filter, login history and account
 * activity recording. Auditing never breaks the audited operation: failures are logged and swallowed.
 */
@Component
public class AuditLogWriter {
    private static final Logger log = LoggerFactory.getLogger(AuditLogWriter.class);

    public static final String AUTHENTICATION = "AUTHENTICATION";
    public static final String ACCOUNT = "ACCOUNT";

    private final JdbcTemplate jdbc;

    public AuditLogWriter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void write(AuditEvent event) {
        try {
            // Actors are recorded by account UUID (the token subject); show their username when it resolves.
            String actor = event.actor();
            UUID actorUuid = event.actorUuid() != null ? event.actorUuid() : parse(actor);
            if (actorUuid != null) {
                String username = jdbc.query("SELECT username FROM users WHERE uuid = ?",
                        rs -> rs.next() ? rs.getString(1) : null, actorUuid);
                if (username != null) actor = username;
            }
            jdbc.update("""
                    INSERT INTO audit_logs (id, occurred_at, category, action, outcome, actor, actor_uuid, target_type,
                        target_id, description, details, ip_address, user_agent, http_method, path, status)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
                    UUID.randomUUID(), Timestamp.from(Instant.now()), cut(event.category(), 40),
                    cut(event.action(), 60), cut(event.outcome() == null ? "SUCCESS" : event.outcome(), 20),
                    cut(actor, 255), actorUuid, cut(event.targetType(), 40), cut(event.targetId(), 255),
                    cut(event.description(), 1000), cut(event.details(), 8000), cut(event.ipAddress(), 64),
                    cut(event.userAgent(), 512), cut(event.httpMethod(), 10), cut(event.path(), 500), event.status());
        } catch (RuntimeException exception) {
            log.warn("Could not write audit log {} {}", event.category(), event.action(), exception);
        }
    }

    static UUID parse(String value) {
        if (value == null) return null;
        try { return UUID.fromString(value); } catch (IllegalArgumentException notUuid) { return null; }
    }

    private static String cut(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }
}
