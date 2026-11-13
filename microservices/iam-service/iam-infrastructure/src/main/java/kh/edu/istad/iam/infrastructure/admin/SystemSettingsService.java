package kh.edu.istad.iam.infrastructure.admin;

import kh.edu.istad.iam.application.admin.AdminModels.*;
import kh.edu.istad.iam.application.admin.SystemSettingsUseCase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Runtime system configuration, stored as one JSON document in system_settings and cached in memory.
 *
 * <p>Where each policy is enforced: authentication (self-registration, social providers, lockout) by the login
 * handlers, registration endpoints and LoginLockoutService; password policy by every endpoint that sets a
 * password; session idle timeout by AccountSessionRegistry and the HTTP session; token lifetimes by the
 * registered clients' token settings, which are rewritten whenever the token policy is saved.
 */
@Service
public class SystemSettingsService implements SystemSettingsUseCase {
    public static final List<String> SOCIAL_PROVIDERS = List.of("google", "github", "facebook", "telegram");
    private static final String KEY = "system";
    private static final String SPECIALS = "!@#$%^&*()-_=+[]{};:'\",.<>/?\\|`~";

    private final JdbcTemplate jdbc;
    private final RegisteredClientRepository clients;
    private final JsonMapper json = JsonMapper.builder().findAndAddModules().build();
    private volatile SystemSettings cached;

    public SystemSettingsService(JdbcTemplate jdbc, RegisteredClientRepository clients) {
        this.jdbc = jdbc;
        this.clients = clients;
    }

    public static SystemSettings defaults() {
        return new SystemSettings(
                new AuthenticationPolicy(true, 5, 15, SOCIAL_PROVIDERS),
                new PasswordPolicy(8, false, false, false, false, 0),
                new SessionPolicy(720),
                new TokenPolicy(60, 30, 5),
                null, "system");
    }

    @Override
    public SystemSettings get() {
        SystemSettings current = cached;
        if (current != null) return current;
        try {
            List<SystemSettings> rows = jdbc.query(
                    "SELECT setting_value FROM system_settings WHERE setting_key = ?",
                    (rs, i) -> json.readValue(rs.getString(1), SystemSettings.class), KEY);
            current = rows.isEmpty() ? defaults() : rows.getFirst();
        } catch (RuntimeException unavailable) {
            // Table not migrated yet (first start) or unreadable value: fall back to defaults.
            current = defaults();
        }
        cached = current;
        return current;
    }

    @Override
    @Transactional
    public SystemSettings update(SystemSettings settings, String actor) {
        SystemSettings valid = validate(settings, actor);
        String value = json.writeValueAsString(valid);
        Timestamp now = Timestamp.from(valid.updatedAt());
        int updated = jdbc.update("UPDATE system_settings SET setting_value = ?, updated_at = ?, updated_by = ? WHERE setting_key = ?",
                value, now, actor, KEY);
        if (updated == 0) {
            jdbc.update("INSERT INTO system_settings (setting_key, setting_value, updated_at, updated_by) VALUES (?, ?, ?, ?)",
                    KEY, value, now, actor);
        }
        TokenPolicy previous = get().tokens();
        cached = valid;
        if (!valid.tokens().equals(previous)) applyTokenPolicyToAllClients(valid.tokens());
        return valid;
    }

    @Override
    public List<String> passwordViolations(String password) {
        PasswordPolicy policy = get().password();
        List<String> violations = new ArrayList<>();
        String value = password == null ? "" : password;
        if (value.length() < policy.minLength()) {
            violations.add("Password must be at least " + policy.minLength() + " characters.");
        }
        if (policy.requireUppercase() && value.chars().noneMatch(Character::isUpperCase)) {
            violations.add("Password must contain an uppercase letter.");
        }
        if (policy.requireLowercase() && value.chars().noneMatch(Character::isLowerCase)) {
            violations.add("Password must contain a lowercase letter.");
        }
        if (policy.requireDigit() && value.chars().noneMatch(Character::isDigit)) {
            violations.add("Password must contain a digit.");
        }
        if (policy.requireSpecial() && value.chars().noneMatch(c -> SPECIALS.indexOf(c) >= 0)) {
            violations.add("Password must contain a special character.");
        }
        return violations;
    }

    /** Applies the token policy to one client's settings (also used for newly registered clients). */
    public TokenSettings tokenSettings(TokenSettings base) {
        TokenPolicy policy = get().tokens();
        return TokenSettings.withSettings(base.getSettings())
                .accessTokenTimeToLive(Duration.ofMinutes(policy.accessTokenMinutes()))
                .refreshTokenTimeToLive(Duration.ofDays(policy.refreshTokenDays()))
                .authorizationCodeTimeToLive(Duration.ofMinutes(policy.authorizationCodeMinutes()))
                .build();
    }

    private void applyTokenPolicyToAllClients(TokenPolicy policy) {
        List<String> ids = jdbc.queryForList("SELECT id FROM oauth2_registered_client", String.class);
        for (String id : ids) {
            RegisteredClient client = clients.findById(id);
            if (client == null) continue;
            clients.save(RegisteredClient.from(client).tokenSettings(tokenSettings(client.getTokenSettings())).build());
        }
    }

    private static SystemSettings validate(SystemSettings s, String actor) {
        if (s == null || s.authentication() == null || s.password() == null || s.session() == null || s.tokens() == null) {
            throw new IllegalArgumentException("authentication, password, session and tokens settings are required.");
        }
        AuthenticationPolicy a = s.authentication();
        range("Maximum failed attempts", a.maxFailedAttempts(), 0, 100);
        range("Lockout duration (minutes)", a.lockoutMinutes(), 0, 10_080);
        List<String> providers = a.enabledSocialProviders().stream().map(String::toLowerCase).distinct().toList();
        for (String provider : providers) {
            if (!SOCIAL_PROVIDERS.contains(provider)) throw new IllegalArgumentException("Unknown provider: " + provider);
        }
        PasswordPolicy p = s.password();
        range("Minimum password length", p.minLength(), 6, 128);
        range("Password maximum age (days)", p.maxAgeDays(), 0, 3650);
        range("Session idle timeout (minutes)", s.session().idleTimeoutMinutes(), 5, 43_200);
        TokenPolicy t = s.tokens();
        range("Access token lifetime (minutes)", t.accessTokenMinutes(), 1, 1440);
        range("Refresh token lifetime (days)", t.refreshTokenDays(), 1, 365);
        range("Authorization code lifetime (minutes)", t.authorizationCodeMinutes(), 1, 30);
        return new SystemSettings(
                new AuthenticationPolicy(a.allowSelfRegistration(), a.maxFailedAttempts(), a.lockoutMinutes(), providers),
                p, s.session(), t, Instant.now(), actor);
    }

    private static void range(String label, int value, int min, int max) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(label + " must be between " + min + " and " + max + ".");
        }
    }

    /** Providers that are both configured (have credentials) and enabled by policy. */
    public boolean providerEnabled(String provider) {
        return provider != null && get().authentication().enabledSocialProviders().contains(provider.toLowerCase());
    }

    public Set<String> enabledProviders() {
        return Set.copyOf(get().authentication().enabledSocialProviders());
    }
}
