package kh.edu.istad.iam.infrastructure.security;

import kh.edu.istad.iam.application.result.AuthUserResult;
import kh.edu.istad.iam.application.result.AuthenticatedUserIdentity;
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
        uuid = user.uuid();
        username = user.username();
        passwordHash = user.passwordHash() == null ? "" : user.passwordHash();
        enabled = user.enabled();
        accountNonExpired = user.accountNonExpired();
        accountNonLocked = user.accountNonLocked();
        credentialsNonExpired = user.credentialsNonExpired();
        authorities = user.authorities().stream().map(SimpleGrantedAuthority::new).toList();
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
