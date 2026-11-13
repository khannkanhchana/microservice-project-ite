package kh.edu.istad.iam.infrastructure.account;

import kh.edu.istad.iam.application.account.*;
import kh.edu.istad.iam.application.port.out.PasswordHasher;
import kh.edu.istad.iam.application.port.out.UserRepository;
import kh.edu.istad.iam.application.result.PageResult;
import kh.edu.istad.iam.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Service
public class AccountSelfServiceService implements AccountSelfServiceUseCase {
    private static final Logger log = LoggerFactory.getLogger(AccountSelfServiceService.class);
    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9._-]{3,50}$");
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern PHONE = Pattern.compile("^\\+?[0-9 ()-]{6,20}$");
    private static final Set<String> PICTURE_TYPES = Set.of("image/png", "image/jpeg", "image/webp", "image/gif");
    private static final int MAX_PICTURE_BYTES = 2 * 1024 * 1024;
    private static final Duration LINK_TICKET_TTL = Duration.ofMinutes(5);
    private static final Duration RESET_TOKEN_TTL = Duration.ofMinutes(30);

    private final JdbcTemplate jdbc;
    private final UserRepository users;
    private final PasswordHasher passwordHasher;
    private final AccountSessionRegistry sessions;
    private final AccountAuditLog audit;
    private final SecureRandom random = new SecureRandom();
    private final Map<String, PendingLink> linkTickets = new ConcurrentHashMap<>();

    public AccountSelfServiceService(JdbcTemplate jdbc, UserRepository users, PasswordHasher passwordHasher,
                                     AccountSessionRegistry sessions, AccountAuditLog audit) {
        this.jdbc = jdbc;
        this.users = users;
        this.passwordHasher = passwordHasher;
        this.sessions = sessions;
        this.audit = audit;
    }

    @Override
    public boolean hasPassword(UUID userUuid) {
        List<String> hashes = jdbc.queryForList("SELECT password_hash FROM users WHERE uuid = ?", String.class, userUuid);
        return !hashes.isEmpty() && hashes.getFirst() != null && !hashes.getFirst().isBlank();
    }

    @Override
    @Transactional
    public List<String> updateContact(UUID userUuid, String username, String email, String phoneNumber, String actor) {
        if (username == null && email == null && phoneNumber == null) return List.of();
        Map<String, Object> current = jdbc.queryForList(
                "SELECT username, email, phone_number FROM users WHERE uuid = ? AND is_deleted = FALSE", userUuid)
                .stream().findFirst().orElseThrow(() -> new IllegalArgumentException("User was not found."));
        List<String> changed = new ArrayList<>();
        String newUsername = (String) current.get("username");
        String newEmail = (String) current.get("email");
        String newPhone = (String) current.get("phone_number");
        boolean resetEmailVerification = false;

        if (username != null && !username.trim().equals(newUsername)) {
            String value = username.trim();
            if (!USERNAME.matcher(value).matches()) {
                throw new IllegalArgumentException("Username must be 3-50 characters: letters, digits, '.', '_' or '-'.");
            }
            if (taken("username", value, userUuid)) throw new IllegalArgumentException("Username is already in use.");
            audit.activity(userUuid, AccountActivityType.USERNAME_CHANGED, newUsername + " → " + value, actor);
            newUsername = value;
            changed.add("username");
        }
        if (email != null && !email.trim().equalsIgnoreCase(newEmail)) {
            String value = email.trim().toLowerCase(Locale.ROOT);
            if (value.length() > 255 || !EMAIL.matcher(value).matches()) {
                throw new IllegalArgumentException("Email address is not valid.");
            }
            if (taken("email", value, userUuid)) throw new IllegalArgumentException("Email is already in use.");
            audit.activity(userUuid, AccountActivityType.EMAIL_CHANGED, mask(newEmail) + " → " + mask(value), actor);
            newEmail = value;
            resetEmailVerification = true;
            changed.add("email");
        }
        if (phoneNumber != null) {
            String value = phoneNumber.isBlank() ? null : phoneNumber.trim();
            if (!Objects.equals(value, newPhone)) {
                if (value != null && !PHONE.matcher(value).matches()) {
                    throw new IllegalArgumentException("Phone number must be 6-20 digits and may start with '+'.");
                }
                if (value != null && taken("phone_number", value, userUuid)) {
                    throw new IllegalArgumentException("Phone number is already in use.");
                }
                audit.activity(userUuid, AccountActivityType.PHONE_CHANGED,
                        value == null ? "removed" : maskPhone(value), actor);
                newPhone = value;
                changed.add("phoneNumber");
            }
        }
        if (changed.isEmpty()) return changed;
        jdbc.update("""
                UPDATE users SET username = ?, email = ?, phone_number = ?,
                    email_verified = CASE WHEN ? THEN FALSE ELSE email_verified END,
                    phone_number_verified = CASE WHEN ? THEN FALSE ELSE phone_number_verified END,
                    updated_at = ?, updated_by = ?, version = version + 1
                WHERE uuid = ?""",
                newUsername, newEmail, newPhone, resetEmailVerification, changed.contains("phoneNumber"),
                Timestamp.from(Instant.now()), actor, userUuid);
        return changed;
    }

    private boolean taken(String column, String value, UUID userUuid) {
        Integer count = jdbc.queryForObject(
                "SELECT count(*) FROM users WHERE lower(" + column + ") = lower(?) AND uuid <> ?",
                Integer.class, value, userUuid);
        return count != null && count > 0;
    }

    @Override
    public List<AccountSessionResult> sessions(UUID userUuid, UUID currentSessionId) {
        // Active sessions are stored in Redis (see AccountSessionRegistry).
        return sessions.list(userUuid).stream()
                .map(s -> new AccountSessionResult(s.id(), s.loginMethod(), s.browser(), s.os(), s.deviceType(),
                        s.ipAddress(), s.location(), s.createdAt(), s.lastActivityAt(),
                        s.id().equals(currentSessionId)))
                .toList();
    }

    @Override
    public void revokeSession(UUID userUuid, UUID sessionId, String actor) {
        if (!sessions.revoke(userUuid, sessionId, "revoked_by_user")) {
            throw new IllegalArgumentException("Session was not found or has already ended.");
        }
        audit.activity(userUuid, AccountActivityType.SESSION_REVOKED, sessionId.toString(), actor);
    }

    @Override
    public int revokeOtherSessions(UUID userUuid, UUID keepSessionId, String actor) {
        int revoked = sessions.revokeAll(userUuid, keepSessionId,
                keepSessionId == null ? "signed_out_everywhere" : "signed_out_other_devices");
        if (revoked > 0) {
            audit.activity(userUuid, AccountActivityType.OTHER_SESSIONS_REVOKED, String.valueOf(revoked), actor);
        }
        return revoked;
    }

    @Override
    public PageResult<LoginAttemptResult> loginHistory(UUID userUuid, Boolean success, int page, int size) {
        int safeSize = Math.clamp(size, 1, 100);
        int safePage = Math.max(page, 0);
        String filter = success == null ? "" : " AND success = " + success;
        Long total = jdbc.queryForObject(
                "SELECT count(*) FROM login_attempts WHERE user_uuid = ?" + filter, Long.class, userUuid);
        List<LoginAttemptResult> content = jdbc.query("""
                SELECT id, success, method, failure_reason, ip_address, location, browser, os, device_type, attempted_at
                FROM login_attempts WHERE user_uuid = ?""" + filter + " ORDER BY attempted_at DESC LIMIT ? OFFSET ?",
                (rs, row) -> new LoginAttemptResult(rs.getObject("id", UUID.class), rs.getBoolean("success"),
                        rs.getString("method"), rs.getString("failure_reason"), rs.getString("ip_address"),
                        rs.getString("location"), rs.getString("browser"), rs.getString("os"),
                        rs.getString("device_type"), instant(rs, "attempted_at")),
                userUuid, safeSize, (long) safePage * safeSize);
        return page(content, safePage, safeSize, total == null ? 0 : total);
    }

    @Override
    public PageResult<AccountActivityResult> activity(UUID userUuid, int page, int size) {
        int safeSize = Math.clamp(size, 1, 100);
        int safePage = Math.max(page, 0);
        Long total = jdbc.queryForObject(
                "SELECT count(*) FROM account_activities WHERE user_uuid = ?", Long.class, userUuid);
        List<AccountActivityResult> content = jdbc.query("""
                SELECT id, activity_type, description, ip_address, browser, os, device_type, occurred_at
                FROM account_activities WHERE user_uuid = ? ORDER BY occurred_at DESC LIMIT ? OFFSET ?""",
                (rs, row) -> new AccountActivityResult(rs.getObject("id", UUID.class), rs.getString("activity_type"),
                        rs.getString("description"), rs.getString("ip_address"), rs.getString("browser"),
                        rs.getString("os"), rs.getString("device_type"), instant(rs, "occurred_at")),
                userUuid, safeSize, (long) safePage * safeSize);
        return page(content, safePage, safeSize, total == null ? 0 : total);
    }

    @Override
    public void recordActivity(UUID userUuid, AccountActivityType type, String description, String actor) {
        audit.activity(userUuid, type, description, actor);
    }

    @Override
    @Transactional
    public void deactivate(UUID userUuid, String password, String reason, String actor) {
        User user = users.findByUuid(userUuid).orElseThrow(() -> new IllegalArgumentException("User was not found."));
        if (user.isDeleted() || !user.isEnabledFlag()) {
            throw new IllegalStateException("This account is already deactivated.");
        }
        String hash = user.getPasswordHash();
        if (hash != null && !hash.isBlank()) {
            if (password == null || password.isBlank() || !passwordHasher.matches(password, hash)) {
                throw new IllegalArgumentException("Password is incorrect.");
            }
        }
        Instant now = Instant.now();
        user.setEnabled(false, now, actor);
        users.save(user);
        jdbc.update("""
                INSERT INTO account_deactivation_requests (id, user_uuid, reason, status, requested_at, processed_at)
                VALUES (?, ?, ?, 'COMPLETED', ?, ?)""",
                UUID.randomUUID(), userUuid, truncate(reason, 500), Timestamp.from(now), Timestamp.from(now));
        audit.activity(userUuid, AccountActivityType.ACCOUNT_DEACTIVATED, truncate(reason, 500), actor);
        sessions.revokeAll(userUuid, null, "account_deactivated");
    }

    @Override
    @Transactional
    public String storePicture(UUID userUuid, String contentType, byte[] data, String publicBaseUrl, String actor) {
        if (data == null || data.length == 0) throw new IllegalArgumentException("Choose an image to upload.");
        if (data.length > MAX_PICTURE_BYTES) throw new IllegalArgumentException("Profile pictures must be 2 MB or smaller.");
        String type = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        if (!PICTURE_TYPES.contains(type) || !looksLikeImage(data)) {
            throw new IllegalArgumentException("Profile pictures must be PNG, JPEG, WebP or GIF images.");
        }
        Instant now = Instant.now();
        jdbc.update("""
                INSERT INTO user_pictures (user_uuid, content_type, data, updated_at) VALUES (?, ?, ?, ?)
                ON CONFLICT (user_uuid) DO UPDATE SET content_type = EXCLUDED.content_type, data = EXCLUDED.data,
                    updated_at = EXCLUDED.updated_at""", userUuid, type, data, Timestamp.from(now));
        String url = publicBaseUrl + "/api/users/" + userUuid + "/picture?v=" + now.toEpochMilli();
        jdbc.update("UPDATE users SET picture_url = ?, updated_at = ?, updated_by = ?, version = version + 1 WHERE uuid = ?",
                url, Timestamp.from(now), actor, userUuid);
        audit.activity(userUuid, AccountActivityType.PICTURE_UPDATED, null, actor);
        return url;
    }

    @Override
    @Transactional
    public void removePicture(UUID userUuid, String actor) {
        jdbc.update("DELETE FROM user_pictures WHERE user_uuid = ?", userUuid);
        jdbc.update("UPDATE users SET picture_url = NULL, updated_at = ?, updated_by = ?, version = version + 1 WHERE uuid = ?",
                Timestamp.from(Instant.now()), actor, userUuid);
        audit.activity(userUuid, AccountActivityType.PICTURE_REMOVED, null, actor);
    }

    @Override
    public Optional<StoredPicture> picture(UUID userUuid) {
        return jdbc.query("SELECT content_type, data FROM user_pictures WHERE user_uuid = ?",
                (rs, row) -> new StoredPicture(rs.getString("content_type"), rs.getBytes("data")), userUuid)
                .stream().findFirst();
    }

    @Override
    public String createLinkTicket(UUID userUuid, String provider, String returnUrl) {
        Instant now = Instant.now();
        linkTickets.values().removeIf(pending -> pending.expiresAt().isBefore(now));
        String ticket = randomToken();
        linkTickets.put(ticket, new PendingLink(new LinkTicket(userUuid, provider, returnUrl), now.plus(LINK_TICKET_TTL)));
        return ticket;
    }

    @Override
    public Optional<LinkTicket> consumeLinkTicket(String ticket) {
        if (ticket == null) return Optional.empty();
        PendingLink pending = linkTickets.remove(ticket);
        if (pending == null || pending.expiresAt().isBefore(Instant.now())) return Optional.empty();
        return Optional.of(pending.ticket());
    }

    @Override
    @Transactional
    public void requestPasswordReset(String email, String resetPageUrl) {
        if (email == null || email.isBlank()) return;
        List<UUID> matches = jdbc.queryForList("""
                SELECT uuid FROM users WHERE lower(email) = lower(?) AND is_deleted = FALSE AND enabled = TRUE""",
                UUID.class, email.trim());
        if (matches.isEmpty()) {
            log.info("Password reset requested for an unknown or inactive email address.");
            return;
        }
        UUID userUuid = matches.getFirst();
        String token = randomToken();
        Instant now = Instant.now();
        jdbc.update("UPDATE password_reset_tokens SET used_at = ? WHERE user_uuid = ? AND used_at IS NULL",
                Timestamp.from(now), userUuid);
        jdbc.update("""
                INSERT INTO password_reset_tokens (token_hash, user_uuid, created_at, expires_at) VALUES (?, ?, ?, ?)""",
                sha256(token), userUuid, Timestamp.from(now), Timestamp.from(now.plus(RESET_TOKEN_TTL)));
        audit.activity(userUuid, AccountActivityType.PASSWORD_RESET_REQUESTED, null, "password-reset");
        // No mail transport is configured for this service; deliver the link through the server log.
        log.warn("Password reset link for {} (valid {} minutes): {}?token={}", mask(email.trim()),
                RESET_TOKEN_TTL.toMinutes(), resetPageUrl, token);
    }

    @Override
    @Transactional
    public void resetPassword(String token, String newPassword) {
        if (token == null || token.isBlank()) throw new IllegalArgumentException("The reset link is invalid or has expired.");
        Instant now = Instant.now();
        List<UUID> owners = jdbc.queryForList("""
                SELECT user_uuid FROM password_reset_tokens
                WHERE token_hash = ? AND used_at IS NULL AND expires_at > ?""",
                UUID.class, sha256(token), Timestamp.from(now));
        if (owners.isEmpty()) throw new IllegalArgumentException("The reset link is invalid or has expired.");
        UUID userUuid = owners.getFirst();
        User user = users.findByUuid(userUuid)
                .orElseThrow(() -> new IllegalArgumentException("The reset link is invalid or has expired."));
        user.changePassword(passwordHasher.hash(newPassword), now, "password-reset");
        users.save(user);
        jdbc.update("UPDATE password_reset_tokens SET used_at = ? WHERE token_hash = ?", Timestamp.from(now), sha256(token));
        audit.activity(userUuid, AccountActivityType.PASSWORD_RESET, null, "password-reset");
        sessions.revokeAll(userUuid, null, "password_reset");
    }

    private String randomToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static boolean looksLikeImage(byte[] data) {
        if (data.length < 12) return false;
        boolean png = (data[0] & 0xFF) == 0x89 && data[1] == 'P' && data[2] == 'N' && data[3] == 'G';
        boolean jpeg = (data[0] & 0xFF) == 0xFF && (data[1] & 0xFF) == 0xD8;
        boolean gif = data[0] == 'G' && data[1] == 'I' && data[2] == 'F';
        boolean webp = data[0] == 'R' && data[1] == 'I' && data[2] == 'F' && data[3] == 'F'
                && data[8] == 'W' && data[9] == 'E' && data[10] == 'B' && data[11] == 'P';
        return png || jpeg || gif || webp;
    }

    private static String mask(String email) {
        if (email == null) return "";
        int at = email.indexOf('@');
        if (at <= 1) return email;
        return email.charAt(0) + "***" + email.substring(at);
    }

    private static String maskPhone(String phone) {
        return phone.length() <= 4 ? phone : "***" + phone.substring(phone.length() - 4);
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }

    private static <T> PageResult<T> page(List<T> content, int page, int size, long total) {
        return new PageResult<>(content, page, size, total, (int) Math.ceil(total / (double) size));
    }

    private static String truncate(String value, int max) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }

    private record PendingLink(LinkTicket ticket, Instant expiresAt) { }
}
