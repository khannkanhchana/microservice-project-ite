package kh.edu.istad.iam.infrastructure.security;

import kh.edu.istad.iam.application.result.UserAuthoritiesResult;
import kh.edu.istad.iam.application.usecase.GetUserAuthoritiesUseCase;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves a user's current authorities (ROLE_* and permission names) and account state for bearer-token API
 * calls. Access tokens are long-lived snapshots; resolving live means role/permission assignments, account
 * deactivation and locks take effect within {@link #TTL} instead of at the next sign-in.
 */
@Component
public class LiveAuthorityResolver {
    private static final Duration TTL = Duration.ofSeconds(10);

    public record LiveAuthorities(boolean active, Set<String> authorities) { }

    private final ObjectProvider<GetUserAuthoritiesUseCase> authorities;
    private final JdbcTemplate jdbc;
    private final Map<UUID, Cached> cache = new ConcurrentHashMap<>();

    public LiveAuthorityResolver(ObjectProvider<GetUserAuthoritiesUseCase> authorities, JdbcTemplate jdbc) {
        this.authorities = authorities;
        this.jdbc = jdbc;
    }

    public LiveAuthorities resolve(UUID userUuid) {
        Cached cached = cache.get(userUuid);
        if (cached != null && cached.at().plus(TTL).isAfter(Instant.now())) return cached.value();
        Boolean active = jdbc.query("""
                SELECT enabled AND account_non_locked AND NOT is_deleted FROM users WHERE uuid = ?""",
                rs -> rs.next() ? rs.getBoolean(1) : null, userUuid);
        LiveAuthorities value;
        if (active == null || !active) {
            value = new LiveAuthorities(false, Set.of());
        } else {
            GetUserAuthoritiesUseCase useCase = authorities.getIfAvailable();
            UserAuthoritiesResult result = useCase == null ? null : useCase.execute(userUuid);
            value = new LiveAuthorities(true, result == null ? Set.of() : Set.copyOf(result.authorities()));
        }
        cache.put(userUuid, new Cached(value, Instant.now()));
        return value;
    }

    /** Drops cached state so the next request sees changes immediately (e.g. after an admin action). */
    public void evict(UUID userUuid) {
        if (userUuid == null) cache.clear();
        else cache.remove(userUuid);
    }

    private record Cached(LiveAuthorities value, Instant at) { }
}
