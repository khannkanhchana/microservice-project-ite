package kh.edu.istad.iam.infrastructure.social;

import kh.edu.istad.iam.application.command.FederatedIdentityCommand;
import kh.edu.istad.iam.application.result.AuthUserResult;
import kh.edu.istad.iam.application.usecase.FindOrProvisionFederatedUserUseCase;
import kh.edu.istad.iam.application.usecase.GetUserUseCase;
import kh.edu.istad.iam.application.usecase.LinkIdentityUseCase;
import kh.edu.istad.iam.application.usecase.LoadUserForAuthenticationUseCase;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Component
public class FederatedUserProvisioner {
    private final ObjectProvider<FindOrProvisionFederatedUserUseCase> findOrProvision;
    private final ObjectProvider<LinkIdentityUseCase> linkIdentity;
    private final ObjectProvider<GetUserUseCase> getUser;
    private final ObjectProvider<LoadUserForAuthenticationUseCase> loadUser;

    public FederatedUserProvisioner(ObjectProvider<FindOrProvisionFederatedUserUseCase> findOrProvision,
                                    ObjectProvider<LinkIdentityUseCase> linkIdentity,
                                    ObjectProvider<GetUserUseCase> getUser,
                                    ObjectProvider<LoadUserForAuthenticationUseCase> loadUser) {
        this.findOrProvision = findOrProvision;
        this.linkIdentity = linkIdentity;
        this.getUser = getUser;
        this.loadUser = loadUser;
    }

    public ProvisionedUser provision(String registrationId, Map<String, Object> providerClaims) {
        String provider = registrationId.toUpperCase(Locale.ROOT);
        String subject = firstString(providerClaims, "sub", "id");
        if (subject == null || subject.isBlank()) {
            throw authenticationError("Provider response did not contain a stable user identifier.");
        }
        String email = firstString(providerClaims, "email");
        String displayName = firstString(providerClaims, "name", "displayName");
        String username = firstString(providerClaims, "preferred_username", "login", "username", "nickname");
        if (username == null || username.isBlank()) username = email == null ? provider.toLowerCase(Locale.ROOT) + "-" + subject : email;
        String givenName = firstString(providerClaims, "given_name", "first_name");
        String familyName = firstString(providerClaims, "family_name", "last_name");
        if ((givenName == null || familyName == null) && displayName != null) {
            String[] names = displayName.trim().split("\\s+", 2);
            if (givenName == null && names.length > 0) givenName = names[0];
            if (familyName == null && names.length > 1) familyName = names[1];
        }
        String picture = picture(providerClaims.get("picture"));
        boolean emailVerified = Boolean.TRUE.equals(providerClaims.get("email_verified"));
        FederatedIdentityCommand command = new FederatedIdentityCommand(provider, subject, email, emailVerified,
                displayName, username, picture, givenName, familyName,
                firstString(providerClaims, "locale", "language_code"), providerClaims,
                Instant.now(), "social:" + provider.toLowerCase(Locale.ROOT));

        try {
            java.util.UUID linkTarget = linkTarget();
            AuthUserResult user = linkTarget == null
                    ? provisionNewOrExisting(command)
                    : linkToCurrentAccount(command, linkTarget);
            Collection<GrantedAuthority> authorities = user.authorities().stream()
                    .map(SimpleGrantedAuthority::new).map(GrantedAuthority.class::cast).toList();
            return new ProvisionedUser(user, authorities);
        } catch (OAuth2AuthenticationException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw authenticationError("The federated account could not be resolved.", exception);
        }
    }

    private AuthUserResult provisionNewOrExisting(FederatedIdentityCommand command) {
        FindOrProvisionFederatedUserUseCase useCase = findOrProvision.getIfAvailable();
        if (useCase == null) throw authenticationError("Federated user provisioning is not configured.");
        return useCase.execute(command);
    }

    private AuthUserResult linkToCurrentAccount(FederatedIdentityCommand command, java.util.UUID userUuid) {
        LinkIdentityUseCase linker = linkIdentity.getIfAvailable();
        GetUserUseCase userLookup = getUser.getIfAvailable();
        LoadUserForAuthenticationUseCase authLookup = loadUser.getIfAvailable();
        if (linker == null || userLookup == null || authLookup == null) {
            throw authenticationError("Social account linking is not configured.");
        }
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        try {
            linker.execute(userUuid, command);
            return authLookup.execute(userLookup.execute(userUuid).username());
        } finally {
            if (attributes != null && attributes.getRequest().getSession(false) != null) {
                attributes.getRequest().getSession(false).removeAttribute("iam.link.userUuid");
            }
        }
    }

    private static java.util.UUID linkTarget() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) return null;
        if (attributes.getRequest().getSession(false) == null) return null;
        Object value = attributes.getRequest().getSession(false).getAttribute("iam.link.userUuid");
        if (value == null) return null;
        try { return java.util.UUID.fromString(value.toString()); }
        catch (IllegalArgumentException ignored) { return null; }
    }

    private static String firstString(Map<String, Object> claims, String... keys) {
        for (String key : keys) {
            Object value = claims.get(key);
            if (value != null && !value.toString().isBlank()) return value.toString();
        }
        return null;
    }

    private static String picture(Object value) {
        if (value instanceof Map<?, ?> map) {
            Object url = map.get("url");
            if (url == null && map.get("data") instanceof Map<?, ?> data) url = data.get("url");
            return url == null ? null : url.toString();
        }
        return value == null ? null : value.toString();
    }

    private static OAuth2AuthenticationException authenticationError(String description) {
        return new OAuth2AuthenticationException(new OAuth2Error("invalid_user_info_response", description, null));
    }

    private static OAuth2AuthenticationException authenticationError(String description, Throwable cause) {
        return new OAuth2AuthenticationException(new OAuth2Error("invalid_user_info_response", description, null), cause);
    }

    public record ProvisionedUser(AuthUserResult user, Collection<GrantedAuthority> authorities) { }
}
