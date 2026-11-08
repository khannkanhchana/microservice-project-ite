package kh.edu.istad.iam.application.command;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Normalized claims supplied by a trusted social-login adapter. */
public record FederatedIdentityCommand(
        String provider,
        String providerSubject,
        String email,
        boolean emailVerified,
        String displayName,
        String username,
        String pictureUrl,
        String givenName,
        String familyName,
        String locale,
        Map<String, Object> attributes,
        Instant authenticatedAt,
        String actor) {
    public FederatedIdentityCommand {
        attributes = attributes == null ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(attributes));
    }
}
