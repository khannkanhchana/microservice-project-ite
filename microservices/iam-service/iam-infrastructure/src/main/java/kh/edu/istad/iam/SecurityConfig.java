package kh.edu.istad.iam;


import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.oauth2.server.authorization.authentication.ClientSecretAuthenticationProvider;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.server.authorization.oidc.authentication.OidcUserInfoAuthenticationContext;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.authorization.*;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import java.util.Set;
import java.util.Map;
import java.util.function.Function;
import kh.edu.istad.iam.infrastructure.oidc.OidcClaimsService;
import kh.edu.istad.iam.infrastructure.oauth.IamAuthorizationJsonMapper;
import kh.edu.istad.iam.infrastructure.security.IamUserDetails;
import kh.edu.istad.iam.infrastructure.social.FederatedOAuth2UserService;
import kh.edu.istad.iam.infrastructure.social.FederatedOidcUserService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private Converter<Jwt, ? extends AbstractAuthenticationToken>
    jwtAuthenticationConverter() {

        JwtAuthenticationConverter converter =
                new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(
                new JwtGrantedAuthoritiesConverter()
        );

        return converter;
    }
    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http,
                                                                       OidcClaimsService oidcClaimsService)
            throws Exception {
        OAuth2AuthorizationServerConfigurer authorizationServerConfigurer =
                new OAuth2AuthorizationServerConfigurer();
        Function<OidcUserInfoAuthenticationContext, OidcUserInfo> userInfoMapper = context -> {
            JwtAuthenticationToken principal = (JwtAuthenticationToken) context.getAuthentication().getPrincipal();
            String subject = principal.getToken().getSubject();
            List<String> requestedScopes = principal.getToken().getClaimAsStringList("scope");
            Set<String> scopes = requestedScopes == null ? Set.of() : Set.copyOf(requestedScopes);
            return new OidcUserInfo(oidcClaimsService.claims(UUID.fromString(subject), scopes));
        };

        http
                .securityMatcher(authorizationServerConfigurer.getEndpointsMatcher())
                .with(authorizationServerConfigurer, (authorizationServer) ->
                        authorizationServer
                                .oidc(oidc -> oidc
                                        .userInfoEndpoint(userInfo -> userInfo.userInfoMapper(userInfoMapper))
                                        .clientRegistrationEndpoint(Customizer.withDefaults()))
                                .clientAuthentication(clientAuthentication ->
                                        clientAuthentication.authenticationProviders(providers -> providers.forEach(provider -> {
                                            if (provider instanceof ClientSecretAuthenticationProvider clientSecretProvider) {
                                                clientSecretProvider.setPasswordEncoder(
                                                        PasswordEncoderFactories.createDelegatingPasswordEncoder());
                                            }
                                        })))
                                .authorizationEndpoint(endpoint -> endpoint.consentPage("/oauth2/consent"))
                )
                .authorizeHttpRequests((authorize) ->
                        authorize
                                .anyRequest().authenticated()
                )
                // Redirect to the login page when not authenticated from the
                // authorization endpoint
                .exceptionHandling((exceptions) -> exceptions
                        .defaultAuthenticationEntryPointFor(
                                new LoginUrlAuthenticationEntryPoint("/login"),
                                new MediaTypeRequestMatcher(MediaType.TEXT_HTML)
                        )
                );

        return http.build();
    }


    @Bean
    @Order(2)
    public SecurityFilterChain defaultSecurityFilterChain(
            HttpSecurity http,
            FederatedOAuth2UserService federatedOAuth2UserService,
            FederatedOidcUserService federatedOidcUserService
    ) throws Exception {

        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/login",
                                "/register",
                                "/api/auth/register",
                                "/oauth2/authorization/**",
                                "/login/social/**",
                                "/login/oauth2/code/**",
                                "/error",
                                "/favicon.ico"
                        ).permitAll()
                        .anyRequest().authenticated()
                )

                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/api/**")
                )

                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
                )

                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .tokenEndpoint(token -> token
                                .accessTokenResponseClient(
                                        kh.edu.istad.iam.infrastructure.social.SocialTokenResponseClient.create()))
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(federatedOAuth2UserService)
                                .oidcUserService(federatedOidcUserService)
                        )
                )

                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .failureUrl("/login?error")
                        .defaultSuccessUrl("/", false)
                        .permitAll()
                )

                .exceptionHandling(exceptions -> exceptions
                        // REST API: បញ្ជូន 401 ជំនួសការបញ្ជូនទៅ Login Page
                        .defaultAuthenticationEntryPointFor(
                                (request, response, exception) -> {
                                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                                    response.setContentType("application/problem+json");
                                    response.getWriter().write(
                                            "{\"type\":\"about:blank\"," +
                                                    "\"title\":\"Unauthorized\"," +
                                                    "\"status\":401," +
                                                    "\"detail\":\"A valid bearer access token is required.\"}"
                                    );
                                },
                                request -> request.getRequestURI()
                                        .startsWith(request.getContextPath() + "/api/")
                        )

                        // Browser: បញ្ជូនអ្នកប្រើប្រាស់ទៅ Login Page
                        .authenticationEntryPoint(
                                new LoginUrlAuthenticationEntryPoint("/login")
                        )

                        .accessDeniedHandler((request, response, exception) -> {
                            response.setStatus(HttpStatus.FORBIDDEN.value());
                            response.setContentType("application/problem+json");
                            response.getWriter().write(
                                    "{\"type\":\"about:blank\"," +
                                            "\"title\":\"Forbidden\"," +
                                            "\"status\":403," +
                                            "\"detail\":\"You do not have permission to perform this action.\"}"
                            );
                        })
                );

        return http.build();
    }


    @Bean
    public RegisteredClientRepository registeredClientRepository(JdbcTemplate jdbcTemplate) {
        return new JdbcRegisteredClientRepository(jdbcTemplate);
    }

    @Bean
    public OAuth2AuthorizationService authorizationService(JdbcTemplate jdbcTemplate,
                                                            RegisteredClientRepository registeredClientRepository) {
        var jsonMapper = IamAuthorizationJsonMapper.create();
        var service = new JdbcOAuth2AuthorizationService(jdbcTemplate, registeredClientRepository);
        service.setAuthorizationParametersMapper(
                new JdbcOAuth2AuthorizationService.JsonMapperOAuth2AuthorizationParametersMapper(jsonMapper));
        service.setAuthorizationRowMapper(
                new JdbcOAuth2AuthorizationService.JsonMapperOAuth2AuthorizationRowMapper(
                        registeredClientRepository, jsonMapper));
        return service;
    }

    @Bean
    public OAuth2AuthorizationConsentService authorizationConsentService(JdbcTemplate jdbcTemplate,
                                                                          RegisteredClientRepository registeredClientRepository) {
        return new JdbcOAuth2AuthorizationConsentService(jdbcTemplate, registeredClientRepository);
    }


    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        KeyPair keyPair = generateRsaKey();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();
        JWKSet jwkSet = new JWKSet(rsaKey);
        return new ImmutableJWKSet<>(jwkSet);
    }

    private static KeyPair generateRsaKey() {
        KeyPair keyPair;
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            keyPair = keyPairGenerator.generateKeyPair();
        }
        catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
        return keyPair;
    }

    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder().build();
    }

    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> jwtTokenCustomizer(OidcClaimsService oidcClaimsService) {
        return context -> {
            if ("access_token".equals(context.getTokenType().getValue())) {
                List<String> authorities = context.getPrincipal().getAuthorities().stream()
                        .map(org.springframework.security.core.GrantedAuthority::getAuthority).distinct().toList();
                context.getClaims().claim("authorities", authorities);
                context.getClaims().claim("roles", authorities.stream()
                        .filter(authority -> authority.startsWith("ROLE_"))
                        .map(authority -> authority.substring("ROLE_".length())).toList());
                context.getClaims().claim("permissions", authorities.stream()
                        .filter(authority -> authority.contains(":"))
                        .toList());
                UUID userUuid = principalUuid(context.getPrincipal().getPrincipal());
                if (userUuid != null) context.getClaims().subject(userUuid.toString());
            } else if (OidcParameterNames.ID_TOKEN.equals(context.getTokenType().getValue())) {
                UUID userUuid = principalUuid(context.getPrincipal().getPrincipal());
                if (userUuid != null) {
                    Map<String, Object> claims = oidcClaimsService.claims(userUuid, context.getAuthorizedScopes());
                    context.getClaims().claims(existing -> existing.putAll(claims));
                    context.getClaims().subject(userUuid.toString());
                }
            }
        };
    }

    private static UUID principalUuid(Object principal) {
        if (principal instanceof IamUserDetails userDetails) return userDetails.uuid();
        if (principal instanceof OAuth2User oauthUser) {
            Object userUuid = oauthUser.getAttributes().get("iam_user_uuid");
            if (userUuid != null) {
                try { return UUID.fromString(userUuid.toString()); }
                catch (IllegalArgumentException ignored) { return null; }
            }
        }
        if (principal instanceof org.springframework.security.core.userdetails.UserDetails userDetails) {
            try { return UUID.fromString(userDetails.getUsername()); }
            catch (IllegalArgumentException ignored) { return null; }
        }
        return null;
    }

}
