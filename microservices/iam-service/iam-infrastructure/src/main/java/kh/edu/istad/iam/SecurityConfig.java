package kh.edu.istad.iam;

import kh.edu.istad.iam.infrastructure.security.LiveAuthorityResolver;


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
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.http.HttpMethod;
import kh.edu.istad.iam.infrastructure.account.AccountLoginHandlers;
import kh.edu.istad.iam.infrastructure.account.AccountSessionFilter;
import kh.edu.istad.iam.infrastructure.account.AccountSessionRegistry;
import kh.edu.istad.iam.infrastructure.account.Principals;
import kh.edu.istad.iam.infrastructure.account.SessionAwareAuthorizationService;
import java.util.Arrays;
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

    /**
     * Rejects bearer tokens whose device session was signed out, records session activity, and exposes
     * the session id to controllers as the {@link AccountSessionRegistry#REQUEST_ATTRIBUTE} request attribute.
     */
    private Converter<Jwt, ? extends AbstractAuthenticationToken>
    jwtAuthenticationConverter(AccountSessionRegistry accountSessions, LiveAuthorityResolver liveAuthorities) {

        // SCOPE_* authorities from the token's scopes, plus — for user tokens — the user's current roles and
        // permissions (ROLE_ADMIN, user:read, ...) resolved live, so @PreAuthorize("hasAuthority('user:read')")
        // works and assignment changes, deactivation and locks apply without waiting for the token to expire.
        JwtGrantedAuthoritiesConverter scopes = new JwtGrantedAuthoritiesConverter();

        return jwt -> {
            String claim = jwt.getClaimAsString(AccountSessionRegistry.TOKEN_CLAIM);
            if (claim != null) {
                UUID sessionId = Principals.parse(claim);
                if (sessionId == null || !accountSessions.isActive(sessionId)) {
                    throw new InvalidBearerTokenException("This session has been signed out.");
                }
                accountSessions.touch(sessionId, null); // bearer-token API call: no browser session
                if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
                    attributes.getRequest().setAttribute(AccountSessionRegistry.REQUEST_ATTRIBUTE, sessionId);
                }
            }
            java.util.List<org.springframework.security.core.GrantedAuthority> granted =
                    new java.util.ArrayList<>(scopes.convert(jwt));
            UUID userUuid = Principals.parse(jwt.getSubject());
            if (userUuid != null) {
                LiveAuthorityResolver.LiveAuthorities live = liveAuthorities.resolve(userUuid);
                if (!live.active()) throw new InvalidBearerTokenException("This account is disabled or locked.");
                live.authorities().forEach(authority -> granted.add(
                        new org.springframework.security.core.authority.SimpleGrantedAuthority(authority)));
            }
            return new org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken(
                    jwt, granted, jwt.getSubject());
        };
    }

    /** Lets the iam-web React app (a different origin) call the REST API and the token endpoint. */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${iam.web.allowed-origins:http://localhost:9000,http://127.0.0.1:9000,http://localhost:5173,http://127.0.0.1:5173}") String allowedOrigins) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.stream(allowedOrigins.split(","))
                .map(String::trim).filter(origin -> !origin.isEmpty()).toList());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "Accept-Language"));
        configuration.setExposedHeaders(List.of("WWW-Authenticate"));
        configuration.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        source.registerCorsConfiguration("/oauth2/token", configuration);
        source.registerCorsConfiguration("/oauth2/revoke", configuration);
        source.registerCorsConfiguration("/userinfo", configuration);
        source.registerCorsConfiguration("/.well-known/**", configuration);
        return source;
    }
    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http,
                                                                       OidcClaimsService oidcClaimsService,
                                                                       AccountSessionRegistry accountSessions)
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
                .cors(Customizer.withDefaults())
                .addFilterBefore(new AccountSessionFilter(accountSessions), AuthorizationFilter.class)
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
            FederatedOidcUserService federatedOidcUserService,
            AccountSessionRegistry accountSessions,
            AccountLoginHandlers loginHandlers,
            LiveAuthorityResolver liveAuthorities
    ) throws Exception {

        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                // iam-web React SPA (static/index.html + assets); its data is protected by /api/**
                                "/",
                                "/index.html",
                                "/assets/**",
                                "/favicon.svg",
                                "/callback",
                                "/account",
                                "/account/**",
                                "/admin",
                                "/admin/**",
                                "/forgot-password",
                                "/reset-password",
                                "/login",
                                "/register",
                                "/api/auth/**",
                                "/oauth2/authorization/**",
                                "/login/social/**",
                                "/login/oauth2/code/**",
                                "/error",
                                "/favicon.ico"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/users/*/picture").permitAll()
                        .anyRequest().authenticated()
                )

                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/api/**")
                )

                .cors(Customizer.withDefaults())
                .addFilterBefore(new AccountSessionFilter(accountSessions), AuthorizationFilter.class)

                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(jwtAuthenticationConverter(accountSessions, liveAuthorities))
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
                        .successHandler(loginHandlers.successHandler())
                        .failureHandler(loginHandlers.failureHandler())
                )

                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .successHandler(loginHandlers.successHandler())
                        .failureHandler(loginHandlers.failureHandler())
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
                                                            RegisteredClientRepository registeredClientRepository,
                                                            AccountSessionRegistry accountSessions) {
        var jsonMapper = IamAuthorizationJsonMapper.create();
        var service = new JdbcOAuth2AuthorizationService(jdbcTemplate, registeredClientRepository);
        service.setAuthorizationParametersMapper(
                new JdbcOAuth2AuthorizationService.JsonMapperOAuth2AuthorizationParametersMapper(jsonMapper));
        service.setAuthorizationRowMapper(
                new JdbcOAuth2AuthorizationService.JsonMapperOAuth2AuthorizationRowMapper(
                        registeredClientRepository, jsonMapper));
        return new SessionAwareAuthorizationService(service, accountSessions);
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
                OAuth2Authorization authorization = context.getAuthorization();
                Object sessionId = authorization == null ? null
                        : authorization.getAttribute(AccountSessionRegistry.AUTHORIZATION_ATTRIBUTE);
                if (sessionId != null) {
                    context.getClaims().claim(AccountSessionRegistry.TOKEN_CLAIM, sessionId.toString());
                }
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
