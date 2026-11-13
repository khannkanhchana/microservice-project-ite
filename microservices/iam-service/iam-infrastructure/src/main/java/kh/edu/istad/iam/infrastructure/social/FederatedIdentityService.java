package kh.edu.istad.iam.infrastructure.social;

import kh.edu.istad.iam.application.command.FederatedIdentityCommand;
import kh.edu.istad.iam.application.port.out.IdentityRepository;
import kh.edu.istad.iam.application.port.out.UserRepository;
import kh.edu.istad.iam.application.result.AuthUserResult;
import kh.edu.istad.iam.application.result.IdentityResult;
import kh.edu.istad.iam.application.usecase.FindOrProvisionFederatedUserUseCase;
import kh.edu.istad.iam.application.usecase.GetUserAuthoritiesUseCase;
import kh.edu.istad.iam.application.usecase.LinkIdentityUseCase;
import kh.edu.istad.iam.application.usecase.ListUserIdentitiesUseCase;
import kh.edu.istad.iam.application.usecase.UnlinkIdentityUseCase;
import kh.edu.istad.iam.domain.Identity;
import kh.edu.istad.iam.domain.User;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Array;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Signs users in through Google, GitHub, Facebook or Telegram: reuses the linked identity when one
 * exists, links to an existing account when the provider has verified the same email address, and
 * otherwise provisions a new local user. Providers do not always supply an email or name, so
 * placeholders are used for the columns the local user model requires.
 */
@Service
public class FederatedIdentityService implements FindOrProvisionFederatedUserUseCase, LinkIdentityUseCase,
        ListUserIdentitiesUseCase, UnlinkIdentityUseCase {
    private static final int USERNAME_MAX = 50;
    private static final int NAME_MAX = 100;
    private static final int URL_MAX = 500;
    private static final String PLACEHOLDER_EMAIL_DOMAIN = "users.noreply.iam.local";

    private final UserRepository users;
    private final IdentityRepository identities;
    private final ObjectProvider<GetUserAuthoritiesUseCase> userAuthorities;

    public FederatedIdentityService(UserRepository users, IdentityRepository identities,
                                    ObjectProvider<GetUserAuthoritiesUseCase> userAuthorities) {
        this.users = users;
        this.identities = identities;
        this.userAuthorities = userAuthorities;
    }

    @Override
    @Transactional
    public AuthUserResult execute(FederatedIdentityCommand command) {
        Instant now = Instant.now();
        Optional<Identity> existing = identities.findByProviderAndSubject(command.provider(), command.providerSubject());
        if (existing.isPresent()) {
            Identity identity = existing.get();
            User user = users.findById(identity.userId())
                    .orElseThrow(() -> error("The linked account no longer exists."));
            identities.save(refresh(identity, command, now));
            return recordLogin(user, now, command.actor());
        }

        String email = normalizedEmail(command.email());
        if (email != null) {
            Optional<User> sameEmail = users.findByUsernameOrEmail(email)
                    .filter(user -> email.equalsIgnoreCase(user.getEmail()));
            if (sameEmail.isPresent()) {
                if (!command.emailVerified()) {
                    throw error("An account with this email already exists. Sign in with your password, then link "
                            + displayProvider(command.provider()) + " from your profile.");
                }
                User user = sameEmail.get();
                link(user, command, now);
                return recordLogin(user, now, command.actor());
            }
        }

        User created = users.save(newUser(command, email, now));
        link(created, command, now);
        return toAuthResult(created);
    }

    @Override
    @Transactional
    public IdentityResult execute(UUID userUuid, FederatedIdentityCommand command) {
        Instant now = Instant.now();
        User user = users.findByUuid(userUuid).orElseThrow(() -> error("The account to link was not found."));
        Optional<Identity> existing = identities.findByProviderAndSubject(command.provider(), command.providerSubject());
        if (existing.isPresent()) {
            Identity identity = existing.get();
            if (!identity.userId().equals(user.getId())) {
                throw error("This " + displayProvider(command.provider()) + " account is already linked to another user.");
            }
            return toResult(identities.save(refresh(identity, command, now)), user.getUuid());
        }
        return toResult(link(user, command, now), user.getUuid());
    }

    @Override
    @Transactional(readOnly = true)
    public List<IdentityResult> execute(UUID userUuid) {
        return identities.findActiveByUserUuid(userUuid).stream()
                .map(identity -> toResult(identity, userUuid)).toList();
    }

    @Override
    @Transactional
    public void execute(UUID userUuid, UUID identityUuid, Instant at, String actor) {
        Identity identity = identities.findByUuid(identityUuid)
                .filter(found -> !found.deleted())
                .orElseThrow(() -> new IllegalArgumentException("Linked account was not found."));
        User user = users.findByUuid(userUuid).orElseThrow(() -> new IllegalArgumentException("User was not found."));
        if (!identity.userId().equals(user.getId())) {
            throw new IllegalArgumentException("Linked account was not found.");
        }
        int methods = identities.countActiveSignInMethods(userUuid);
        if (methods <= 1) {
            throw new IllegalStateException("Set a password or link another account before unlinking your only sign-in method.");
        }
        identities.save(identity.unlink(at, actor, methods));
    }

    private Identity link(User user, FederatedIdentityCommand command, Instant now) {
        if (identities.existsActiveByUserUuidAndProvider(user.getUuid(), command.provider())) {
            throw error("Your account is already linked to a different " + displayProvider(command.provider()) + " account.");
        }
        Identity identity = new Identity(null, UUID.randomUUID(), user.getId(), command.provider(),
                command.providerSubject(), truncate(command.email(), 255), command.emailVerified(),
                truncate(command.displayName(), 150), truncate(command.username(), 100), url(command.pictureUrl()),
                jsonSafe(command.attributes()), now, now, now, command.actor(), now, command.actor(),
                false, null, null, 0);
        return identities.save(identity);
    }

    private static Identity refresh(Identity identity, FederatedIdentityCommand command, Instant now) {
        Identity active = identity.deleted() ? identity.restore(now, command.actor()) : identity;
        return active.updateProfile(truncate(command.email(), 255), command.emailVerified(),
                        truncate(command.displayName(), 150), truncate(command.username(), 100),
                        url(command.pictureUrl()), jsonSafe(command.attributes()), now, command.actor())
                .recordLogin(now, command.actor());
    }

    private AuthUserResult recordLogin(User user, Instant now, String actor) {
        if (!user.isEnabled() || !user.isAccountNonLocked()) {
            throw error("This account is disabled or locked.");
        }
        user.recordSuccessfulLogin(now, actor);
        return toAuthResult(users.save(user));
    }

    private User newUser(FederatedIdentityCommand command, String email, Instant now) {
        String provider = command.provider().toLowerCase(Locale.ROOT);
        String userEmail = email != null ? email
                : (provider + "-" + command.providerSubject().replaceAll("[^A-Za-z0-9]", "") + "@" + PLACEHOLDER_EMAIL_DOMAIN)
                .toLowerCase(Locale.ROOT);

        String givenName = firstNonBlank(command.givenName(), firstWord(command.displayName()), command.username(), provider);
        String familyName = firstNonBlank(command.familyName(), remainingWords(command.displayName()), "-");

        return User.builder()
                .username(uniqueUsername(command, email, provider))
                .givenName(truncate(givenName, NAME_MAX))
                .familyName(truncate(familyName, NAME_MAX))
                .nickname(truncate(command.displayName(), NAME_MAX))
                .locale(locale(command.locale()))
                .pictureUrl(url(command.pictureUrl()))
                .email(userEmail)
                .emailVerified(email != null && command.emailVerified())
                .createdAt(now)
                .createdBy(command.actor())
                .lastLoginAt(now)
                .build();
    }

    private String uniqueUsername(FederatedIdentityCommand command, String email, String provider) {
        String requested = command.username();
        if (requested != null && requested.contains("@")) requested = requested.substring(0, requested.indexOf('@'));
        String raw = firstNonBlank(requested,
                email == null ? null : email.substring(0, email.indexOf('@')),
                provider + "-" + command.providerSubject());
        String base = raw.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]", "_").replaceAll("_+", "_");
        if (base.length() < 3) base = provider + "_" + base;
        base = truncate(base, USERNAME_MAX - 7);
        String candidate = base;
        while (users.existsByUsername(candidate)) {
            candidate = base + "-" + UUID.randomUUID().toString().substring(0, 6);
        }
        return candidate;
    }

    private AuthUserResult toAuthResult(User user) {
        GetUserAuthoritiesUseCase lookup = userAuthorities.getIfAvailable();
        Set<String> authorities = lookup != null
                ? lookup.execute(user.getUuid()).authorities()
                : user.getAuthorities().stream().map(authority -> authority.getAuthority())
                        .collect(Collectors.toUnmodifiableSet());
        return new AuthUserResult(user.getUuid(), user.getUsername(), user.getPasswordHash(), user.isEnabled(),
                user.isAccountNonExpired(), user.isAccountNonLocked(), user.isCredentialsNonExpired(), authorities);
    }

    private static IdentityResult toResult(Identity identity, UUID userUuid) {
        return new IdentityResult(identity.uuid(), userUuid, identity.provider(), identity.providerSubject(),
                identity.email(), identity.emailVerified(), identity.displayName(), identity.username(),
                identity.pictureUrl(), identity.attributes(), identity.linkedAt(), identity.lastLoginAt(),
                identity.createdAt(), identity.createdBy(), identity.updatedAt(), identity.updatedBy(),
                identity.deleted(), identity.deletedAt(), identity.deletedBy(), identity.version());
    }

    /** ID-token claims contain Instant and URL values; the identity attributes column only accepts JSON types. */
    private static Map<String, Object> jsonSafe(Map<String, Object> attributes) {
        Map<String, Object> copy = new LinkedHashMap<>();
        attributes.forEach((key, value) -> copy.put(key, jsonValue(value)));
        return copy;
    }

    private static Object jsonValue(Object value) {
        if (value == null || value instanceof String || value instanceof Boolean || value instanceof Number) return value;
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> copy = new LinkedHashMap<>();
            map.forEach((key, nested) -> copy.put(String.valueOf(key), jsonValue(nested)));
            return copy;
        }
        if (value instanceof Collection<?> collection) {
            List<Object> copy = new ArrayList<>(collection.size());
            collection.forEach(item -> copy.add(jsonValue(item)));
            return copy;
        }
        if (value.getClass().isArray()) {
            List<Object> copy = new ArrayList<>();
            for (int i = 0; i < Array.getLength(value); i++) copy.add(jsonValue(Array.get(value, i)));
            return copy;
        }
        return value.toString();
    }

    private static String normalizedEmail(String email) {
        if (email == null || email.isBlank()) return null;
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        return normalized.length() <= 255 && normalized.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$") ? normalized : null;
    }

    private static String locale(String locale) {
        if (locale == null || locale.isBlank() || locale.trim().length() > 10) return "en";
        return locale.trim();
    }

    private static String url(String value) {
        return value == null || value.isBlank() || value.length() > URL_MAX ? null : value;
    }

    private static String firstWord(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim().split("\\s+", 2)[0];
    }

    private static String remainingWords(String value) {
        if (value == null || value.isBlank()) return null;
        String[] parts = value.trim().split("\\s+", 2);
        return parts.length > 1 ? parts[1] : null;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) if (value != null && !value.isBlank()) return value.trim();
        return null;
    }

    private static String truncate(String value, int max) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }

    private static String displayProvider(String provider) {
        String lower = provider.toLowerCase(Locale.ROOT);
        return switch (lower) {
            case "github" -> "GitHub";
            default -> Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
        };
    }

    private static OAuth2AuthenticationException error(String description) {
        return new OAuth2AuthenticationException(new OAuth2Error("invalid_user_info_response", description, null), description);
    }
}
