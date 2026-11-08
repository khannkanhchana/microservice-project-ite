package kh.edu.istad.iam.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** A third-party provider account linked to a local user. */
public record Identity(
        Long id,
        UUID uuid,
        Long userId,
        String provider,
        String providerSubject,
        String email,
        boolean emailVerified,
        String displayName,
        String username,
        String pictureUrl,
        Map<String, Object> attributes,
        Instant linkedAt,
        Instant lastLoginAt,
        Instant createdAt,
        String createdBy,
        Instant updatedAt,
        String updatedBy,
        boolean deleted,
        Instant deletedAt,
        String deletedBy,
        long version) {

    public Identity {
        Objects.requireNonNull(uuid, "uuid is required");
        Objects.requireNonNull(userId, "userId is required");
        if (userId <= 0) throw new IllegalArgumentException("userId must be positive");
        provider = required(provider, "provider", 20).toUpperCase(Locale.ROOT);
        providerSubject = required(providerSubject, "providerSubject", 255);
        email = optional(email, "email", 255);
        displayName = optional(displayName, "displayName", 150);
        username = optional(username, "username", 100);
        pictureUrl = optional(pictureUrl, "pictureUrl", 500);
        attributes = immutableJsonObject(attributes);
        Objects.requireNonNull(linkedAt, "linkedAt is required");
        Objects.requireNonNull(createdAt, "createdAt is required");
        createdBy = required(createdBy, "createdBy", 255);
        Objects.requireNonNull(updatedAt, "updatedAt is required");
        updatedBy = required(updatedBy, "updatedBy", 255);
        if (updatedAt.isBefore(createdAt)) throw new IllegalArgumentException("updatedAt cannot be before createdAt");
        if (lastLoginAt != null && lastLoginAt.isBefore(linkedAt)) {
            throw new IllegalArgumentException("lastLoginAt cannot be before linkedAt");
        }
        if (deleted != (deletedAt != null)) throw new IllegalArgumentException("deletedAt must be set exactly when deleted is true");
        if (deleted != (deletedBy != null)) throw new IllegalArgumentException("deletedBy must be set exactly when deleted is true");
        if (deletedAt != null && deletedAt.isBefore(createdAt)) throw new IllegalArgumentException("deletedAt cannot be before createdAt");
        if (deletedBy != null) deletedBy = required(deletedBy, "deletedBy", 255);
        if (version < 0) throw new IllegalArgumentException("version cannot be negative");
    }

    public Identity recordLogin(Instant at, String actor) {
        Objects.requireNonNull(at, "login timestamp is required");
        if (deleted) throw new IllegalStateException("Cannot sign in through a deleted identity");
        if (at.isBefore(linkedAt) || at.isBefore(updatedAt)) {
            throw new IllegalArgumentException("login timestamp cannot precede linkedAt or updatedAt");
        }
        String auditActor = required(actor, "updatedBy", 255);
        return copy(email, emailVerified, displayName, username, pictureUrl, attributes,
                at, at, auditActor, false, null, null, version + 1);
    }

    public Identity updateProfile(String email, boolean emailVerified, String displayName,
                                 String username, String pictureUrl, Map<String, Object> attributes,
                                 Instant at, String actor) {
        requireActive();
        validateUpdateTime(at);
        return copy(email, emailVerified, displayName, username, pictureUrl, attributes,
                lastLoginAt, at, actor, false, null, null, version + 1);
    }

    /** Verifies the active provider is not already linked to the same user. */
    public static void validateProviderLink(Long userId, String provider, Collection<Identity> existingIdentities) {
        Objects.requireNonNull(userId, "userId is required");
        if (userId <= 0) throw new IllegalArgumentException("userId must be positive");
        String providerKey = required(provider, "provider", 20).toUpperCase(Locale.ROOT);
        Objects.requireNonNull(existingIdentities, "existingIdentities is required");
        boolean alreadyLinked = existingIdentities.stream()
                .filter(Objects::nonNull)
                .anyMatch(identity -> identity.userId.equals(userId)
                        && identity.provider.equals(providerKey)
                        && !identity.deleted);
        if (alreadyLinked) throw new IllegalStateException("A user can have only one active identity per provider");
    }

    /** Unlinks this provider only when another sign-in method will remain available. */
    public Identity unlink(Instant at, String actor, int activeSignInMethodsBeforeUnlink) {
        requireActive();
        if (activeSignInMethodsBeforeUnlink <= 1) {
            throw new IllegalStateException("A user cannot unlink their last sign-in method");
        }
        validateUpdateTime(at);
        return copy(email, emailVerified, displayName, username, pictureUrl, attributes,
                lastLoginAt, at, actor, true, at, actor, version + 1);
    }

    public Identity restore(Instant at, String actor) {
        if (!deleted) throw new IllegalStateException("Identity is not deleted");
        validateUpdateTime(at);
        return copy(email, emailVerified, displayName, username, pictureUrl, attributes,
                lastLoginAt, at, actor, false, null, null, version + 1);
    }

    private Identity copy(String email, boolean emailVerified, String displayName, String username,
                          String pictureUrl, Map<String, Object> attributes, Instant lastLoginAt,
                          Instant updatedAt, String actor, boolean deleted, Instant deletedAt,
                          String deletedBy, long version) {
        return new Identity(id, uuid, userId, provider, providerSubject, email, emailVerified,
                displayName, username, pictureUrl, attributes, linkedAt, lastLoginAt,
                createdAt, createdBy, updatedAt, required(actor, "updatedBy", 255),
                deleted, deletedAt, deletedBy, version);
    }

    private void requireActive() {
        if (deleted) throw new IllegalStateException("Identity has been deleted");
    }

    private void validateUpdateTime(Instant at) {
        Objects.requireNonNull(at, "audit timestamp is required");
        if (at.isBefore(updatedAt)) throw new IllegalArgumentException("audit timestamp cannot precede updatedAt");
    }

    private static Map<String, Object> immutableJsonObject(Map<String, Object> values) {
        if (values == null || values.isEmpty()) return Map.of();
        Map<String, Object> copy = new LinkedHashMap<>();
        values.forEach((key, value) -> copy.put(required(key, "attribute name", 255), immutableJsonValue(value)));
        return Collections.unmodifiableMap(copy);
    }

    private static Object immutableJsonValue(Object value) {
        if (value == null || value instanceof String || value instanceof Boolean || value instanceof Number) return value;
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> copy = new LinkedHashMap<>();
            map.forEach((key, nested) -> {
                if (!(key instanceof String stringKey)) throw new IllegalArgumentException("JSON object keys must be strings");
                copy.put(stringKey, immutableJsonValue(nested));
            });
            return Collections.unmodifiableMap(copy);
        }
        if (value instanceof List<?> list) {
            List<Object> copy = new ArrayList<>(list.size());
            list.forEach(item -> copy.add(immutableJsonValue(item)));
            return Collections.unmodifiableList(copy);
        }
        throw new IllegalArgumentException("attributes must contain JSON-compatible values");
    }

    private static String required(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        String normalized = value.trim();
        if (normalized.length() > maxLength) throw new IllegalArgumentException(field + " cannot exceed " + maxLength + " characters");
        return normalized;
    }

    private static String optional(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim();
        if (normalized.length() > maxLength) throw new IllegalArgumentException(field + " cannot exceed " + maxLength + " characters");
        return normalized;
    }
}
