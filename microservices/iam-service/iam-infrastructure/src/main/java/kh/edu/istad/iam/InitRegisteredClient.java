package kh.edu.istad.iam;


import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.UUID;

@RequiredArgsConstructor
@Configuration
public class InitRegisteredClient {
    private static final Logger logger = LoggerFactory.getLogger(InitRegisteredClient.class);

    private final RegisteredClientRepository registeredClientRepository;
    private final JdbcTemplate jdbcTemplate;

    private static final String OIDC_CLIENT_ID = "oidc-client";
    private static final String OIDC_CLIENT_SECRET = "secret";
    private final PasswordEncoder clientSecretEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();

    @Value("${iam.authorization-server.dcr.registrar-client-id:registrar-client}")
    private String registrarClientId;

    @Value("${iam.authorization-server.dcr.registrar-client-secret:}")
    private String registrarClientSecret;


    @PostConstruct
    public void init(){

        TokenSettings tokenSettings = TokenSettings.builder()
                .accessTokenFormat(OAuth2TokenFormat.SELF_CONTAINED)
                .accessTokenTimeToLive(Duration.ofHours(5))
                .refreshTokenTimeToLive(Duration.ofDays(7))
                .authorizationCodeTimeToLive(Duration.ofMinutes(3))
                .reuseRefreshTokens(false)
                .build();

        ClientSettings clientSettings = ClientSettings
                .builder()
                .requireProofKey(true)
                .requireAuthorizationConsent(true)
                .build();



        RegisteredClient oidcClient = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(OIDC_CLIENT_ID)
                .clientSecret(clientSecretEncoder.encode(OIDC_CLIENT_SECRET))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri("http://127.0.0.1:8080/login/oauth2/code/oidc-client")
                .redirectUri(
                        "https://oauth.pstmn.io/v1/callback"
                )
                .postLogoutRedirectUri("http://127.0.0.1:8080/")
                .scope(OidcScopes.OPENID)
                .scope(OidcScopes.PROFILE)
                .scope(OidcScopes.EMAIL)
                .scope(OidcScopes.PHONE)
                .scope(OidcScopes.ADDRESS)
                .clientSettings(clientSettings)
                .tokenSettings(tokenSettings)
                .build();
        var client = registeredClientRepository.findByClientId(OIDC_CLIENT_ID);
        if (client == null) {
            registeredClientRepository.save(oidcClient);
            logger.info("Registered the local Postman OAuth client.");
        } else if (!clientSecretMatches(OIDC_CLIENT_SECRET, client.getClientSecret())) {
            // Reconcile only the local Postman client's secret; preserve its ID and all existing grants.
            jdbcTemplate.update("UPDATE oauth2_registered_client SET client_secret = ? WHERE id = ?",
                    oidcClient.getClientSecret(), client.getId());
            logger.warn("Repaired the local Postman OAuth client secret to match the configured development credential.");
        } else {
            logger.info("Verified the local Postman OAuth client secret.");
        }

        if (registrarClientSecret != null && !registrarClientSecret.isBlank()) {
            var existingRegistrarClient = registeredClientRepository.findByClientId(registrarClientId);
            RegisteredClient registrarClient = RegisteredClient.withId(UUID.randomUUID().toString())
                    .clientId(registrarClientId)
                    .clientSecret(clientSecretEncoder.encode(registrarClientSecret))
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                    .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                    .scope("client.create")
                    .scope("client.read")
                    .clientSettings(ClientSettings.builder().requireAuthorizationConsent(false).build())
                    .tokenSettings(tokenSettings)
                    .build();
            if (existingRegistrarClient == null) {
                registeredClientRepository.save(registrarClient);
            } else if (!clientSecretMatches(registrarClientSecret, existingRegistrarClient.getClientSecret())) {
                jdbcTemplate.update("UPDATE oauth2_registered_client SET client_secret = ? WHERE id = ?",
                        registrarClient.getClientSecret(), existingRegistrarClient.getId());
                logger.warn("Repaired the configured registrar OAuth client secret encoding.");
            }
        }
    }

    private boolean clientSecretMatches(String rawSecret, String storedSecret) {
        if (storedSecret == null || !storedSecret.startsWith("{")) {
            return false;
        }
        try {
            return clientSecretEncoder.matches(rawSecret, storedSecret);
        } catch (IllegalArgumentException unsupportedEncoding) {
            return false;
        }
    }


}
