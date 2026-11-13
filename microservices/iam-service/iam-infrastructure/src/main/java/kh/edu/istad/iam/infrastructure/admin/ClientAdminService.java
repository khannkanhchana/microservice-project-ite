package kh.edu.istad.iam.infrastructure.admin;

import kh.edu.istad.iam.application.admin.AdminModels.ClientCommand;
import kh.edu.istad.iam.application.admin.AdminModels.ClientDetails;
import kh.edu.istad.iam.application.admin.AdminModels.ClientWithSecret;
import kh.edu.istad.iam.application.admin.ClientAdminUseCase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * OAuth2/OIDC client administration on top of the authorization server's JDBC RegisteredClientRepository.
 * Secrets are generated here, stored only as a hash, and returned in plain text once (on create/rotate).
 */
@Service
public class ClientAdminService implements ClientAdminUseCase {
    public static final List<String> GRANT_TYPES = List.of("authorization_code", "refresh_token", "client_credentials");
    public static final List<String> AUTH_METHODS = List.of("client_secret_basic", "client_secret_post", "none");
    private static final Pattern CLIENT_ID = Pattern.compile("^[A-Za-z0-9._-]{3,100}$");
    private static final Pattern SCOPE = Pattern.compile("^[A-Za-z0-9:._-]{1,100}$");

    private final RegisteredClientRepository clients;
    private final JdbcTemplate jdbc;
    private final SystemSettingsService settings;
    private final PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
    private final SecureRandom random = new SecureRandom();
    private final Set<String> systemClients;

    public ClientAdminService(RegisteredClientRepository clients, JdbcTemplate jdbc, SystemSettingsService settings,
                              @Value("${iam.authorization-server.dcr.registrar-client-id:registrar-client}") String registrar) {
        this.clients = clients;
        this.jdbc = jdbc;
        this.settings = settings;
        // Registered by InitRegisteredClient on every start; the console must not delete or rename them.
        this.systemClients = Set.of("iam-web", "oidc-client", registrar);
    }

    @Override
    public List<ClientDetails> list(String search) {
        String like = search == null || search.isBlank() ? null : "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
        List<String> ids = like == null
                ? jdbc.queryForList("SELECT id FROM oauth2_registered_client", String.class)
                : jdbc.queryForList("SELECT id FROM oauth2_registered_client WHERE lower(client_id) LIKE ? OR lower(client_name) LIKE ?",
                        String.class, like, like);
        List<ClientDetails> result = new ArrayList<>();
        for (String id : ids) {
            RegisteredClient client = clients.findById(id);
            if (client != null) result.add(details(client));
        }
        result.sort(Comparator.comparing(ClientDetails::system).reversed().thenComparing(ClientDetails::clientId));
        return result;
    }

    @Override
    public ClientDetails get(String id) {
        return details(require(id));
    }

    @Override
    @Transactional
    public ClientWithSecret create(ClientCommand command, String actor) {
        String clientId = command.clientId() == null ? "" : command.clientId().trim();
        if (!CLIENT_ID.matcher(clientId).matches()) {
            throw new IllegalArgumentException("Client ID must be 3-100 characters: letters, digits, '.', '_' or '-'.");
        }
        if (clients.findByClientId(clientId) != null) throw new IllegalArgumentException("Client ID is already in use.");

        RegisteredClient.Builder builder = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(clientId)
                .clientIdIssuedAt(Instant.now())
                .tokenSettings(settings.tokenSettings(TokenSettings.builder()
                        .accessTokenFormat(OAuth2TokenFormat.SELF_CONTAINED).reuseRefreshTokens(false).build()));
        String secret = apply(builder, command, null);
        RegisteredClient client = builder.build();
        clients.save(client);
        return new ClientWithSecret(details(client), secret);
    }

    @Override
    @Transactional
    public ClientWithSecret update(String id, ClientCommand command, String actor) {
        RegisteredClient existing = require(id);
        RegisteredClient.Builder builder = RegisteredClient.from(existing);
        String secret = apply(builder, command, existing);
        if (secret != null) builder.clientSecret(encoder.encode(secret)); // became confidential: issue a secret
        RegisteredClient updated = builder.build();
        clients.save(updated);
        return new ClientWithSecret(details(updated), secret);
    }

    @Override
    @Transactional
    public void delete(String id, String actor) {
        RegisteredClient client = require(id);
        if (systemClients.contains(client.getClientId())) {
            throw new IllegalArgumentException("Built-in client '" + client.getClientId() + "' cannot be deleted.");
        }
        jdbc.update("DELETE FROM oauth2_authorization_consent WHERE registered_client_id = ?", id);
        jdbc.update("DELETE FROM oauth2_authorization WHERE registered_client_id = ?", id);
        jdbc.update("DELETE FROM oauth2_registered_client WHERE id = ?", id);
    }

    @Override
    @Transactional
    public ClientWithSecret rotateSecret(String id, String actor) {
        RegisteredClient client = require(id);
        if (!confidential(client)) throw new IllegalArgumentException("Public clients have no secret.");
        String secret = newSecret();
        RegisteredClient updated = RegisteredClient.from(client).clientSecret(encoder.encode(secret)).build();
        clients.save(updated);
        return new ClientWithSecret(details(updated), secret);
    }

    /** Applies and validates the editable settings; returns a new plain secret when one must be issued. */
    private String apply(RegisteredClient.Builder builder, ClientCommand c, RegisteredClient existing) {
        String name = c.clientName() == null || c.clientName().isBlank() ? null : c.clientName().trim();
        if (name == null || name.length() > 200) throw new IllegalArgumentException("Client name is required (max 200 characters).");
        if (existing != null && c.clientId() != null && !c.clientId().isBlank()
                && !c.clientId().trim().equals(existing.getClientId())) {
            throw new IllegalArgumentException("Client ID cannot be changed.");
        }

        List<String> requestedMethods = normalize(c.authenticationMethods());
        List<String> methods = requestedMethods.isEmpty() ? List.of("client_secret_basic") : requestedMethods;
        for (String m : methods) {
            if (!AUTH_METHODS.contains(m)) throw new IllegalArgumentException("Unsupported authentication method: " + m);
        }
        boolean isPublic = methods.contains("none");
        if (isPublic && methods.size() > 1) {
            throw new IllegalArgumentException("A public client (none) cannot also use a client secret method.");
        }

        List<String> grants = normalize(c.grantTypes());
        if (grants.isEmpty()) throw new IllegalArgumentException("Select at least one grant type.");
        for (String g : grants) {
            if (!GRANT_TYPES.contains(g)) throw new IllegalArgumentException("Unsupported grant type: " + g);
        }
        if (isPublic && grants.contains("client_credentials")) {
            throw new IllegalArgumentException("Public clients cannot use the client_credentials grant.");
        }
        if (grants.contains("refresh_token") && !grants.contains("authorization_code")) {
            throw new IllegalArgumentException("refresh_token requires the authorization_code grant.");
        }

        List<String> redirects = uris(c.redirectUris(), "Redirect URI");
        List<String> logoutRedirects = uris(c.postLogoutRedirectUris(), "Post-logout redirect URI");
        if (grants.contains("authorization_code") && redirects.isEmpty()) {
            throw new IllegalArgumentException("The authorization_code grant needs at least one redirect URI.");
        }
        List<String> scopes = normalizeKeepCase(c.scopes());
        for (String s : scopes) {
            if (!SCOPE.matcher(s).matches()) throw new IllegalArgumentException("Invalid scope: " + s);
        }
        if (scopes.isEmpty()) throw new IllegalArgumentException("Select at least one scope.");

        builder.clientName(name)
                .clientAuthenticationMethods(set -> { set.clear(); methods.forEach(m -> set.add(new ClientAuthenticationMethod(m))); })
                .authorizationGrantTypes(set -> { set.clear(); grants.forEach(g -> set.add(new AuthorizationGrantType(g))); })
                .redirectUris(set -> { set.clear(); set.addAll(redirects); })
                .postLogoutRedirectUris(set -> { set.clear(); set.addAll(logoutRedirects); })
                .scopes(set -> { set.clear(); set.addAll(scopes); })
                .clientSettings(ClientSettings.builder()
                        .requireProofKey(isPublic || flag(c.requireProofKey(),
                                existing != null && existing.getClientSettings().isRequireProofKey()))
                        .requireAuthorizationConsent(flag(c.requireConsent(),
                                existing != null && existing.getClientSettings().isRequireAuthorizationConsent()))
                        .build());

        TokenSettings base = existing == null
                ? settings.tokenSettings(TokenSettings.builder().accessTokenFormat(OAuth2TokenFormat.SELF_CONTAINED)
                        .reuseRefreshTokens(false).build())
                : existing.getTokenSettings();
        TokenSettings.Builder tokens = TokenSettings.withSettings(base.getSettings());
        if (c.accessTokenMinutes() != null) {
            if (c.accessTokenMinutes() < 1 || c.accessTokenMinutes() > 1440) throw new IllegalArgumentException("Access token lifetime must be 1-1440 minutes.");
            tokens.accessTokenTimeToLive(Duration.ofMinutes(c.accessTokenMinutes()));
        }
        if (c.refreshTokenDays() != null) {
            if (c.refreshTokenDays() < 1 || c.refreshTokenDays() > 365) throw new IllegalArgumentException("Refresh token lifetime must be 1-365 days.");
            tokens.refreshTokenTimeToLive(Duration.ofDays(c.refreshTokenDays()));
        }
        if (c.reuseRefreshTokens() != null) tokens.reuseRefreshTokens(c.reuseRefreshTokens());
        builder.tokenSettings(tokens.build());

        if (isPublic) {
            builder.clientSecret(null);
            return null;
        }
        boolean hadSecret = existing != null && existing.getClientSecret() != null;
        if (hadSecret) return null;
        String secret = newSecret();
        if (existing == null) builder.clientSecret(encoder.encode(secret));
        return secret;
    }

    /** Missing flags keep the current value on update (false for a new client). */
    private static boolean flag(Boolean requested, boolean current) {
        return requested == null ? current : requested;
    }

    private ClientDetails details(RegisteredClient c) {
        TokenSettings t = c.getTokenSettings();
        Long authorizations = jdbc.queryForObject(
                "SELECT count(*) FROM oauth2_authorization WHERE registered_client_id = ?", Long.class, c.getId());
        return new ClientDetails(c.getId(), c.getClientId(), c.getClientName(), c.getClientIdIssuedAt(), confidential(c),
                c.getClientSecretExpiresAt(),
                c.getClientAuthenticationMethods().stream().map(ClientAuthenticationMethod::getValue).sorted().toList(),
                c.getAuthorizationGrantTypes().stream().map(AuthorizationGrantType::getValue).sorted().toList(),
                c.getRedirectUris().stream().sorted().toList(), c.getPostLogoutRedirectUris().stream().sorted().toList(),
                c.getScopes().stream().sorted().toList(), c.getClientSettings().isRequireProofKey(),
                c.getClientSettings().isRequireAuthorizationConsent(), t.getAccessTokenTimeToLive().toMinutes(),
                t.getRefreshTokenTimeToLive().toDays(), t.isReuseRefreshTokens(),
                t.getAuthorizationCodeTimeToLive().toMinutes(), systemClients.contains(c.getClientId()),
                authorizations == null ? 0 : authorizations);
    }

    private static boolean confidential(RegisteredClient c) {
        return !c.getClientAuthenticationMethods().contains(ClientAuthenticationMethod.NONE);
    }

    private RegisteredClient require(String id) {
        RegisteredClient client = id == null ? null : clients.findById(id);
        if (client == null) throw new IllegalArgumentException("Client was not found.");
        return client;
    }

    private String newSecret() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static List<String> normalize(List<String> values) {
        Set<String> out = new LinkedHashSet<>();
        for (String v : values) if (v != null && !v.isBlank()) out.add(v.trim().toLowerCase(Locale.ROOT));
        return List.copyOf(out);
    }

    private static List<String> normalizeKeepCase(List<String> values) {
        Set<String> out = new LinkedHashSet<>();
        for (String v : values) if (v != null && !v.isBlank()) out.add(v.trim());
        return List.copyOf(out);
    }

    private static List<String> uris(List<String> values, String label) {
        List<String> out = normalizeKeepCase(values);
        for (String uri : out) {
            try {
                URI parsed = URI.create(uri);
                if (parsed.getScheme() == null || parsed.getFragment() != null) throw new IllegalArgumentException();
            } catch (IllegalArgumentException invalid) {
                throw new IllegalArgumentException(label + " must be an absolute URI without a fragment: " + uri);
            }
        }
        return out;
    }
}
