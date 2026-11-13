package kh.edu.istad.iam.infrastructure.security;

import kh.edu.istad.iam.application.result.AuthUserResult;
import kh.edu.istad.iam.infrastructure.admin.LoginLockoutService;

import kh.edu.istad.iam.application.usecase.LoadUserForAuthenticationUseCase;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Component
public class UseCaseUserDetailsService implements UserDetailsService {
    private final ObjectProvider<LoadUserForAuthenticationUseCase> loadUser;
    private final LoginLockoutService lockout;

    public UseCaseUserDetailsService(ObjectProvider<LoadUserForAuthenticationUseCase> loadUser,
                                     LoginLockoutService lockout) {
        this.loadUser = loadUser;
        this.lockout = lockout;
    }

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        LoadUserForAuthenticationUseCase useCase = loadUser.getIfAvailable();
        if (useCase == null) {
            throw new UsernameNotFoundException("Authentication use case is not wired.");
        }
        lockout.releaseExpiredLock(usernameOrEmail); // timed lockout from too many failed attempts
        try {
            AuthUserResult user = useCase.execute(usernameOrEmail);
            if (user.credentialsNonExpired() && lockout.passwordExpired(user.uuid())) {
                // Password older than the policy's maximum age: sign-in is refused until it is reset.
                user = new AuthUserResult(user.uuid(), user.username(), user.passwordHash(), user.enabled(),
                        user.accountNonExpired(), user.accountNonLocked(), false, user.authorities());
            }
            return new IamUserDetails(user);
        } catch (RuntimeException exception) {
            throw new UsernameNotFoundException("User was not found or is not available for authentication.", exception);
        }
    }
}
