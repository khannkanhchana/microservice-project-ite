package kh.edu.istad.iam.infrastructure.security;

import kh.edu.istad.iam.application.result.AuthUserResult;
import kh.edu.istad.iam.application.result.AuthenticatedUserIdentity;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.UUID;

public final class IamUserDetails implements UserDetails, AuthenticatedUserIdentity {
    private final UUID uuid;
    private final String username;
    private final String passwordHash;
    private final boolean enabled;
    private final boolean accountNonExpired;
    private final boolean accountNonLocked;
    private final boolean credentialsNonExpired;
    private final Collection<? extends GrantedAuthority> authorities;

    public IamUserDetails(AuthUserResult user) {
        this(user.uuid(), user.username(), user.passwordHash(), user.enabled(), user.accountNonExpired(),
                user.accountNonLocked(), user.credentialsNonExpired(),
                user.authorities().stream().map(SimpleGrantedAuthority::new).toList());
    }

    /** Reconstructs the authenticated principal stored in JDBC OAuth2 authorization attributes. */
    @JsonCreator
    public IamUserDetails(@JsonProperty("userUuid") UUID uuid,
                          @JsonProperty("username") String username,
                          @JsonProperty("password") String passwordHash,
                          @JsonProperty("enabled") boolean enabled,
                          @JsonProperty("accountNonExpired") boolean accountNonExpired,
                          @JsonProperty("accountNonLocked") boolean accountNonLocked,
                          @JsonProperty("credentialsNonExpired") boolean credentialsNonExpired,
                          @JsonProperty("authorities") Collection<? extends GrantedAuthority> authorities) {
        this.uuid = uuid;
        this.username = username;
        this.passwordHash = passwordHash == null ? "" : passwordHash;
        this.enabled = enabled;
        this.accountNonExpired = accountNonExpired;
        this.accountNonLocked = accountNonLocked;
        this.credentialsNonExpired = credentialsNonExpired;
        this.authorities = authorities == null ? java.util.List.of() : java.util.List.copyOf(authorities);
    }

    public UUID uuid() { return uuid; }
    @Override public UUID getUserUuid() { return uuid; }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    @Override public String getPassword() { return passwordHash; }
    @Override public String getUsername() { return username; }
    @Override public boolean isAccountNonExpired() { return accountNonExpired; }
    @Override public boolean isAccountNonLocked() { return accountNonLocked; }
    @Override public boolean isCredentialsNonExpired() { return credentialsNonExpired; }
    @Override public boolean isEnabled() { return enabled; }
}
