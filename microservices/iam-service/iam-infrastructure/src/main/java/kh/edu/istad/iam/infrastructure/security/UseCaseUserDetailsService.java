package kh.edu.istad.iam.infrastructure.security;

import kh.edu.istad.iam.application.usecase.LoadUserForAuthenticationUseCase;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Component
public class UseCaseUserDetailsService implements UserDetailsService {
    private final ObjectProvider<LoadUserForAuthenticationUseCase> loadUser;

    public UseCaseUserDetailsService(ObjectProvider<LoadUserForAuthenticationUseCase> loadUser) {
        this.loadUser = loadUser;
    }

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        LoadUserForAuthenticationUseCase useCase = loadUser.getIfAvailable();
        if (useCase == null) {
            throw new UsernameNotFoundException("Authentication use case is not wired.");
        }
        try {
            return new IamUserDetails(useCase.execute(usernameOrEmail));
        } catch (RuntimeException exception) {
            throw new UsernameNotFoundException("User was not found or is not available for authentication.", exception);
        }
    }
}
