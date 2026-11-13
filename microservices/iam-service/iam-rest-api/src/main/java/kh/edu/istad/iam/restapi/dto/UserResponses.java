package kh.edu.istad.iam.restapi.dto;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class UserResponses {
    private UserResponses() { }

    public record UserResponse(UUID uuid, String username, String email, boolean emailVerified,
                               String name, String locale, boolean enabled, List<Role> roles,
                               Instant createdAt, Instant updatedAt) {
        public UserResponse { roles = List.copyOf(roles); }
    }

    public record Role(UUID uuid, String name) { }

    public record UserAuthoritiesResponse(UUID userUuid, List<RoleAuthority> roles,
                                          List<DirectPermission> directPermissions,
                                          Set<String> effectivePermissions) {
        public UserAuthoritiesResponse {
            roles = List.copyOf(roles);
            directPermissions = List.copyOf(directPermissions);
            effectivePermissions = Set.copyOf(effectivePermissions);
        }
    }

    public record RoleAuthority(UUID uuid, String name, Instant expiresAt) { }

    public record DirectPermission(UUID uuid, String name, String effect, Instant expiresAt) { }

    public record IdentityResponse(UUID uuid, String provider, String email, String displayName,
                                   String username, String pictureUrl, Instant linkedAt, Instant lastLoginAt) { }

    /** The signed-in user's own account, including everything the profile page can edit. */
    public record MeResponse(UUID uuid, String username, String email, boolean emailVerified,
                             String name, String givenName, String familyName, String middleName, String nickname,
                             String gender, java.time.LocalDate birthdate, String locale, String zoneinfo,
                             String pictureUrl, String websiteUrl, String phoneNumber, boolean phoneNumberVerified,
                             Address address, boolean enabled, boolean hasPassword, List<Role> roles,
                             Set<String> permissions, Instant lastLoginAt, Instant passwordChangedAt,
                             Instant createdAt, Instant updatedAt) {
        public MeResponse {
            roles = List.copyOf(roles);
            permissions = Set.copyOf(permissions);
        }
    }

    public record Address(String streetAddress, String locality, String region, String postalCode, String country) { }

    public record AuthProvider(String id, String name) { }

    public record LinkStart(String authorizationUrl) { }

    public record PictureUpload(String pictureUrl) { }

    public record SessionsRevoked(int revoked) { }
}
