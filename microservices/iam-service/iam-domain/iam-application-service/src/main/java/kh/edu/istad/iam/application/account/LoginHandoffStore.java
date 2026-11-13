package kh.edu.istad.iam.application.account;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Single-use, short-lived tickets that carry a social login across two hostnames of this same server.
 *
 * <p>A provider such as Telegram only accepts an HTTPS callback, so its redirect-uri is the public ngrok URL
 * while users browse http://localhost:9000. Browser sessions are per host, so the login is started on the
 * public host with an "outbound" ticket (where to return, and any account-link in progress) and the signed-in
 * result is brought back to localhost with an "inbound" ticket. Tickets live in memory, are random 256-bit
 * values, can be redeemed once, and expire after {@link #TTL}.
 */
public final class LoginHandoffStore {
    public static final Duration TTL = Duration.ofMinutes(5);

    /** Payload key: origin (scheme://host[:port]) the user started on and must return to. */
    public static final String RETURN_ORIGIN = "returnOrigin";
    /** Payload key: Map&lt;String, Object&gt; of session attributes to copy into the session on the other host. */
    public static final String SESSION_ATTRIBUTES = "sessionAttributes";
    /** Payload key: the Spring Security Authentication produced by the social login (inbound only). */
    public static final String AUTHENTICATION = "authentication";
    /** Session attribute on the public host: where to hand the finished login back to. */
    public static final String RETURN_ORIGIN_SESSION_ATTRIBUTE = "iam.handoff.returnOrigin";

    private final SecureRandom random = new SecureRandom();
    private final Map<String, Entry> tickets = new ConcurrentHashMap<>();
    private final Clock clock;

    public LoginHandoffStore() {
        this(Clock.systemUTC());
    }

    public LoginHandoffStore(Clock clock) {
        this.clock = clock;
    }

    /** Stores the payload and returns the ticket that redeems it. */
    public String issue(Map<String, Object> payload) {
        purgeExpired();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String ticket = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        tickets.put(ticket, new Entry(Map.copyOf(payload), clock.instant().plus(TTL)));
        return ticket;
    }

    /** Returns the payload once; unknown, reused or expired tickets yield empty. */
    public Optional<Map<String, Object>> redeem(String ticket) {
        if (ticket == null || ticket.isBlank()) return Optional.empty();
        Entry entry = tickets.remove(ticket);
        if (entry == null || clock.instant().isAfter(entry.expiresAt())) return Optional.empty();
        return Optional.of(entry.payload());
    }

    private void purgeExpired() {
        Instant now = clock.instant();
        tickets.entrySet().removeIf(e -> now.isAfter(e.getValue().expiresAt()));
    }

    private record Entry(Map<String, Object> payload, Instant expiresAt) { }
}
