package kh.edu.istad.iam.infrastructure.security;

import kh.edu.istad.iam.application.port.out.UserRepository;
import kh.edu.istad.iam.application.result.AuthUserResult;
import kh.edu.istad.iam.application.usecase.LoadUserForAuthenticationUseCase;
import kh.edu.istad.iam.domain.User;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

/** Loads persisted IAM credentials and account state for Spring Security login. */
@Service
public class LoadUserForAuthenticationService implements LoadUserForAuthenticationUseCase {
    private final UserRepository users;

    public LoadUserForAuthenticationService(UserRepository users) {
        this.users = users;
    }

    @Override
    public AuthUserResult execute(String usernameOrEmail) {
        User user = users.findByUsernameOrEmail(usernameOrEmail)
                .orElseThrow(() -> new IllegalArgumentException("User was not found or is not available for authentication."));

        return new AuthUserResult(
                user.getUuid(),
                user.getUsername(),
                user.getPasswordHash(),
                user.isEnabled(),
                user.isAccountNonExpired(),
                user.isAccountNonLocked(),
                user.isCredentialsNonExpired(),
                user.getAuthorities().stream()
                        .map(authority -> authority.getAuthority())
                        .collect(Collectors.toUnmodifiableSet()));
    }
}
