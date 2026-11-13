package kh.edu.istad.iam.infrastructure.admin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Locale;

/**
 * Grants the ADMIN role to the accounts listed in {@code iam.admin.bootstrap-users} (usernames or emails,
 * comma-separated; env IAM_BOOTSTRAP_ADMINS) at startup, so a fresh installation has an administrator.
 * Accounts that already hold an active ADMIN assignment are left untouched.
 */
@Component
public class AdminBootstrap implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final JdbcTemplate jdbc;
    private final String users;

    public AdminBootstrap(JdbcTemplate jdbc, @Value("${iam.admin.bootstrap-users:}") String users) {
        this.jdbc = jdbc;
        this.users = users;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (users == null || users.isBlank()) return;
        Arrays.stream(users.split(",")).map(String::trim).filter(v -> !v.isEmpty()).forEach(this::grant);
    }

    private void grant(String identifier) {
        String value = identifier.toLowerCase(Locale.ROOT);
        int granted = jdbc.update("""
                INSERT INTO user_roles (user_id, role_id, assigned_at, assigned_by, reason)
                SELECT u.id, r.id, now(), 'bootstrap', 'iam.admin.bootstrap-users'
                FROM users u CROSS JOIN roles r
                WHERE (lower(u.username) = ? OR lower(u.email) = ?) AND u.is_deleted = FALSE AND r.name = 'ADMIN'
                  AND NOT EXISTS (SELECT 1 FROM user_roles ur WHERE ur.user_id = u.id AND ur.role_id = r.id
                                  AND ur.revoked_at IS NULL AND (ur.expires_at IS NULL OR ur.expires_at > now()))""",
                value, value);
        if (granted > 0) log.info("Granted the ADMIN role to {}.", identifier);
        else if (jdbc.queryForObject("SELECT count(*) FROM users WHERE lower(username) = ? OR lower(email) = ?",
                Long.class, value, value) == 0) {
            log.warn("iam.admin.bootstrap-users: no account matches '{}'.", identifier);
        }
    }
}
