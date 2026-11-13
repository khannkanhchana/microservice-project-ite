package kh.edu.istad.iam.infrastructure.account;

import org.springframework.beans.factory.ObjectProvider;
import kh.edu.istad.iam.infrastructure.admin.SystemSettingsService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.session.SessionRepository;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Tracks device sessions in Redis. A session starts when a browser signs in to the IAM server (password or
 * social login), is stored in that browser's HTTP session (itself kept in Redis by Spring Session) under
 * {@link #SESSION_ATTRIBUTE}, is copied onto every OAuth2 authorization created from that browser, and is
 * carried in issued access tokens as the {@link #TOKEN_CLAIM} claim.
 *
 * <p>Redis layout (all keys expire after the session idle timeout of the System Configuration):
 * <ul>
 *   <li>{@code iam:account-session:{id}} — hash: user, login method, device, browser, OS, IP, location,
 *       created/last-activity time and the Spring Session id of the browser;</li>
 *   <li>{@code iam:account-session:{id}:authorizations} — set of OAuth2 authorization ids issued from it;</li>
 *   <li>{@code iam:user-sessions:{userUuid}} — set of the user's session ids (index for "active sessions").</li>
 * </ul>
 * Revoking a session deletes those keys, deletes the browser's Spring Session (so it must sign in again), and
 * deletes its OAuth2 authorizations — which rejects its bearer tokens and refresh tokens.
 */
@Component
public class AccountSessionRegistry {
    public static final String SESSION_ATTRIBUTE = "iam.sid";
    public static final String AUTHORIZATION_ATTRIBUTE = "iam_sid";
    public static final String TOKEN_CLAIM = "iam_sid";
    public static final String REQUEST_ATTRIBUTE = "iam.currentSessionId";

    private static final String SESSION_KEY = "iam:account-session:";
    private static final String AUTHORIZATIONS_SUFFIX = ":authorizations";
    private static final String USER_KEY = "iam:user-sessions:";
    private static final Duration TOUCH_INTERVAL = Duration.ofSeconds(30);

    private final StringRedisTemplate redis;
    private final JdbcTemplate jdbc;
    private final ObjectProvider<SessionRepository<?>> httpSessions;
    private final SystemSettingsService settings;

    public AccountSessionRegistry(StringRedisTemplate redis, JdbcTemplate jdbc,
                                  ObjectProvider<SessionRepository<?>> httpSessions, SystemSettingsService settings) {
        this.redis = redis;
        this.jdbc = jdbc;
        this.httpSessions = httpSessions;
        this.settings = settings;
    }

    /** Session expiration policy (System Configuration), read on every use so changes apply immediately. */
    private Duration idleTimeout() {
        return Duration.ofMinutes(settings.get().session().idleTimeoutMinutes());
    }

    /** Number of users with at least one active session, and the total number of active sessions. */
    public long[] activeCounts() {
        long users = 0;
        long total = 0;
        try (var cursor = redis.scan(org.springframework.data.redis.core.ScanOptions.scanOptions()
                .match(USER_KEY + "*").count(500).build())) {
            while (cursor.hasNext()) {
                String key = cursor.next();
                Long size = redis.opsForSet().size(key);
                if (size != null && size > 0) {
                    UUID user = UUID.fromString(key.substring(USER_KEY.length()));
                    long live = list(user).size(); // prunes expired entries
                    if (live > 0) { users++; total += live; }
                }
            }
        }
        return new long[] { users, total };
    }

    /** One active device session as stored in Redis. */
    public record DeviceSession(UUID id, UUID userUuid, String loginMethod, String ipAddress, String userAgent,
                                String browser, String os, String deviceType, String location,
                                Instant createdAt, Instant lastActivityAt, String httpSessionId) { }

    public UUID start(UUID userUuid, String loginMethod, ClientInfo client, String httpSessionId) {
        UUID id = UUID.randomUUID();
        String now = String.valueOf(Instant.now().toEpochMilli());
        Map<String, String> fields = new HashMap<>();
        fields.put("userUuid", userUuid.toString());
        put(fields, "loginMethod", loginMethod);
        put(fields, "ipAddress", client.ipAddress());
        put(fields, "userAgent", client.userAgent());
        put(fields, "browser", client.browser());
        put(fields, "os", client.os());
        put(fields, "deviceType", client.deviceType());
        put(fields, "location", client.location());
        put(fields, "httpSessionId", httpSessionId);
        fields.put("createdAt", now);
        fields.put("lastActivityAt", now);

        redis.opsForHash().putAll(sessionKey(id), fields);
        redis.expire(sessionKey(id), idleTimeout());
        redis.opsForSet().add(userKey(userUuid), id.toString());
        redis.expire(userKey(userUuid), idleTimeout());
        return id;
    }

    /** Returns the owner when the session exists (not revoked, not expired), otherwise null. */
    public UUID activeOwner(UUID sessionId) {
        if (sessionId == null) return null;
        Object owner = redis.opsForHash().get(sessionKey(sessionId), "userUuid");
        return owner == null ? null : UUID.fromString(owner.toString());
    }

    public boolean isActive(UUID sessionId) {
        return activeOwner(sessionId) != null;
    }

    /** Records activity (throttled) and extends the idle timeout; also tracks the browser's current session id. */
    public void touch(UUID sessionId, String httpSessionId) {
        if (sessionId == null) return;
        String key = sessionKey(sessionId);
        Object last = redis.opsForHash().get(key, "lastActivityAt");
        if (last == null) return;
        Instant now = Instant.now();
        if (httpSessionId != null && !httpSessionId.equals(redis.opsForHash().get(key, "httpSessionId"))) {
            redis.opsForHash().put(key, "httpSessionId", httpSessionId); // e.g. after session-fixation id change
        }
        if (Instant.ofEpochMilli(Long.parseLong(last.toString())).plus(TOUCH_INTERVAL).isAfter(now)) return;

        redis.opsForHash().put(key, "lastActivityAt", String.valueOf(now.toEpochMilli()));
        redis.expire(key, idleTimeout());
        redis.expire(key + AUTHORIZATIONS_SUFFIX, idleTimeout());
        Object owner = redis.opsForHash().get(key, "userUuid");
        if (owner != null) redis.expire(userKey(UUID.fromString(owner.toString())), idleTimeout());
    }

    public void bindAuthorization(String authorizationId, UUID sessionId) {
        if (!isActive(sessionId)) return;
        String key = sessionKey(sessionId) + AUTHORIZATIONS_SUFFIX;
        redis.opsForSet().add(key, authorizationId);
        redis.expire(key, idleTimeout());
    }

    /** The user's active sessions, most recently used first. Expired entries are pruned from the index. */
    public List<DeviceSession> list(UUID userUuid) {
        Set<String> ids = redis.opsForSet().members(userKey(userUuid));
        if (ids == null || ids.isEmpty()) return List.of();
        List<DeviceSession> result = new ArrayList<>();
        for (String id : ids) {
            DeviceSession session = read(UUID.fromString(id));
            if (session == null || !userUuid.equals(session.userUuid())) {
                redis.opsForSet().remove(userKey(userUuid), id);
            } else {
                result.add(session);
            }
        }
        result.sort(Comparator.comparing(DeviceSession::lastActivityAt).reversed());
        return result;
    }

    /** Revokes one session owned by {@code userUuid}; returns false when it does not exist or is already ended. */
    public boolean revoke(UUID userUuid, UUID sessionId, String reason) {
        DeviceSession session = read(sessionId);
        if (session == null || !userUuid.equals(session.userUuid())) return false;
        end(session);
        return true;
    }

    /** Revokes every active session of the user except {@code keep} (null revokes all). */
    public int revokeAll(UUID userUuid, UUID keep, String reason) {
        int revoked = 0;
        for (DeviceSession session : list(userUuid)) {
            if (session.id().equals(keep)) continue;
            end(session);
            revoked++;
        }
        return revoked;
    }

    private void end(DeviceSession session) {
        String key = sessionKey(session.id());
        Set<String> authorizations = redis.opsForSet().members(key + AUTHORIZATIONS_SUFFIX);
        redis.delete(List.of(key, key + AUTHORIZATIONS_SUFFIX));
        redis.opsForSet().remove(userKey(session.userUuid()), session.id().toString());

        // Sign the browser out of the IAM server: its Spring Session lives in Redis too.
        SessionRepository<?> repository = httpSessions.getIfAvailable();
        if (repository != null && session.httpSessionId() != null) repository.deleteById(session.httpSessionId());

        // Its OAuth2 authorizations (and so its refresh tokens) are in PostgreSQL.
        if (authorizations != null) {
            for (String authorizationId : authorizations) {
                jdbc.update("DELETE FROM oauth2_authorization WHERE id = ?", authorizationId);
            }
        }
    }

    private DeviceSession read(UUID id) {
        Map<Object, Object> f = redis.opsForHash().entries(sessionKey(id));
        if (f == null || f.get("userUuid") == null) return null;
        return new DeviceSession(id, UUID.fromString(str(f, "userUuid")), str(f, "loginMethod"),
                str(f, "ipAddress"), str(f, "userAgent"), str(f, "browser"), str(f, "os"), str(f, "deviceType"),
                str(f, "location"), instant(f, "createdAt"), instant(f, "lastActivityAt"), str(f, "httpSessionId"));
    }

    private static String sessionKey(UUID id) {
        return SESSION_KEY + id;
    }

    private static String userKey(UUID userUuid) {
        return USER_KEY + userUuid;
    }

    private static void put(Map<String, String> map, String key, String value) {
        if (value != null) map.put(key, value);
    }

    private static String str(Map<Object, Object> fields, String key) {
        Object value = fields.get(key);
        return value == null ? null : value.toString();
    }

    private static Instant instant(Map<Object, Object> fields, String key) {
        String value = str(fields, key);
        return value == null ? Instant.EPOCH : Instant.ofEpochMilli(Long.parseLong(value));
    }
}
