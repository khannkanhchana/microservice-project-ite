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

    public record IdentityResponse(UUID uuid, String provider, String email,
                                   String displayName, Instant linkedAt) { }
}
