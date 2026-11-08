package kh.edu.istad.iam.application.result;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public record IdentityResult(
        UUID uuid, UUID userUuid, String provider, String providerSubject,
        String email, boolean emailVerified, String displayName, String username,
        String pictureUrl, Map<String, Object> attributes, Instant linkedAt, Instant lastLoginAt,
        Instant createdAt, String createdBy, Instant updatedAt, String updatedBy,
        boolean deleted, Instant deletedAt, String deletedBy, long version) {
    public IdentityResult {
        attributes = attributes == null ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(attributes));
    }
}
