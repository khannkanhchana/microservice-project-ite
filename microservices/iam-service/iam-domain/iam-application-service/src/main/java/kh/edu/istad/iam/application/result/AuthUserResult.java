package kh.edu.istad.iam.application.result;

import java.util.Set;
import java.util.UUID;

/** Credential and account state needed by the authentication adapter; never expose from REST. */
public record AuthUserResult(UUID uuid, String username, String passwordHash,
                             boolean enabled, boolean accountNonExpired,
                             boolean accountNonLocked, boolean credentialsNonExpired,
                             Set<String> authorities) {
    public AuthUserResult {
        authorities = Set.copyOf(authorities);
    }
}
