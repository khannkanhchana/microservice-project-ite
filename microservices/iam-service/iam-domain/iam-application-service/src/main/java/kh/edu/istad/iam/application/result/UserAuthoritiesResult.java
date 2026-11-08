package kh.edu.istad.iam.application.result;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record UserAuthoritiesResult(UUID userUuid, List<RoleAuthority> roles,
                                    List<DirectPermission> directPermissions,
                                    Set<String> effectivePermissions, Set<String> authorities) {
    public UserAuthoritiesResult {
        roles = List.copyOf(roles);
        directPermissions = List.copyOf(directPermissions);
        effectivePermissions = Set.copyOf(effectivePermissions);
        authorities = Set.copyOf(authorities);
    }

    public record RoleAuthority(UUID uuid, String name, Instant expiresAt) { }

    public record DirectPermission(UUID uuid, String name,
                                   kh.edu.istad.iam.domain.AssignmentEffect effect,
                                   Instant expiresAt) { }
}
