package kh.edu.istad.platform.customer.config;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import kh.edu.istad.platform.customer.domain.usecase.GetAllCustomersUseCase;
import kh.edu.istad.platform.customer.domain.usecase.InitiateCustomerUseCase;
import kh.edu.istad.platform.customer.domain.usecase.QueryCustomersUseCase;
import kh.edu.istad.platform.customer.restapi.controller.CustomerController;
import kh.edu.istad.platform.customer.restapi.mapper.CustomerWebMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.config.EnableWebFlux;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

import java.time.Instant;
import java.util.Date;

import static org.mockito.Mockito.*;

@SpringJUnitConfig(CustomerSecurityTests.TestConfig.class)
class CustomerSecurityTests {
    private static final String ISSUER = "http://localhost:9000";
    private static final RSAKey KEY;
    private static final DisposableServer JWKS;

    static {
        try {
            KEY = new RSAKeyGenerator(2048).keyID("test-key").generate();
            String publicKeys = new JWKSet(KEY.toPublicJWK()).toString();
            JWKS = HttpServer.create().host("127.0.0.1").port(0)
                    .route(routes -> routes.get("/oauth2/jwks", (request, response) ->
                            response.header("Content-Type", "application/json")
                                    .sendString(Mono.just(publicKeys))))
                    .bindNow();
        } catch (Exception exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    @DynamicPropertySource
    static void jwtProperties(DynamicPropertyRegistry properties) {
        properties.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> ISSUER);
        properties.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
                () -> "http://127.0.0.1:" + JWKS.port() + "/oauth2/jwks");
    }

    @Configuration
    @EnableWebFlux
    @Import({SecurityConfig.class, CustomerController.class})
    static class TestConfig {
        @Bean
        GetAllCustomersUseCase getAll() {
            var useCase = mock(GetAllCustomersUseCase.class);
            when(useCase.execute()).thenReturn(Flux.empty());
            return useCase;
        }

        @Bean
        QueryCustomersUseCase query() {
            var useCase = mock(QueryCustomersUseCase.class);
            when(useCase.execute(null, null, null)).thenReturn(Flux.empty());
            return useCase;
        }

        @Bean
        InitiateCustomerUseCase create() {
            return mock(InitiateCustomerUseCase.class);
        }

        @Bean
        CustomerWebMapper mapper() {
            return mock(CustomerWebMapper.class);
        }
    }

    @Autowired
    ApplicationContext context;
    private WebTestClient client;

    @BeforeEach
    void setUp() {
        client = WebTestClient.bindToApplicationContext(context).build();
    }

    @AfterAll
    static void stopJwks() {
        JWKS.disposeNow();
    }

    @Test
    void allowsReadScopeOnBothCustomerGetRoutes() throws Exception {
        String token = token(KEY, ISSUER, "customer.read", 300, -10);
        for (String path : new String[]{"/api/customers", "/api/customers/query"}) {
            client.get().uri(path).headers(headers -> headers.setBearerAuth(token))
                    .exchange().expectStatus().isOk().expectBody().json("[]");
        }
    }

    @Test
    void rejectsMissingTokenWithBearerChallenge() {
        client.get().uri("/api/customers").exchange().expectStatus().isUnauthorized()
                .expectHeader().valueMatches(HttpHeaders.WWW_AUTHENTICATE, "Bearer.*");
    }

    @Test
    void rejectsMalformedToken() {
        getWithToken("not-a-jwt").expectStatus().isUnauthorized();
    }

    @Test
    void rejectsExpiredToken() throws Exception {
        getWithToken(token(KEY, ISSUER, "customer.read", -120, -600))
                .expectStatus().isUnauthorized();
    }

    @Test
    void rejectsFutureNotBefore() throws Exception {
        getWithToken(token(KEY, ISSUER, "customer.read", 600, 120))
                .expectStatus().isUnauthorized();
    }

    @Test
    void rejectsWrongIssuer() throws Exception {
        getWithToken(token(KEY, "https://untrusted.example", "customer.read", 300, -10))
                .expectStatus().isUnauthorized();
    }

    @Test
    void rejectsWrongSignature() throws Exception {
        var otherKey = new RSAKeyGenerator(2048).keyID("test-key").generate();
        getWithToken(token(otherKey, ISSUER, "customer.read", 300, -10))
                .expectStatus().isUnauthorized();
    }

    @Test
    void rejectsValidTokenWithoutReadScope() throws Exception {
        getWithToken(token(KEY, ISSUER, "other.read", 300, -10))
                .expectStatus().isForbidden();
    }

    @Test
    void readScopeCannotCreateCustomer() throws Exception {
        String token = token(KEY, ISSUER, "customer.read", 300, -10);
        client.post().uri("/api/customers")
                .headers(headers -> headers.setBearerAuth(token))
                .exchange().expectStatus().isForbidden();
        verifyNoInteractions(context.getBean(InitiateCustomerUseCase.class));
    }

    @Test
    void missingTokenCannotCreateCustomer() {
        client.post().uri("/api/customers").exchange().expectStatus().isUnauthorized();
    }

    private WebTestClient.ResponseSpec getWithToken(String token) {
        return client.get().uri("/api/customers")
                .headers(headers -> headers.setBearerAuth(token)).exchange();
    }

    private static String token(RSAKey key, String issuer, String scope,
                                long expiresIn, long notBeforeIn) throws Exception {
        Instant now = Instant.now();
        var claims = new JWTClaimsSet.Builder().issuer(issuer).subject("test-client")
                .issueTime(Date.from(now.minusSeconds(600)))
                .expirationTime(Date.from(now.plusSeconds(expiresIn)))
                .notBeforeTime(Date.from(now.plusSeconds(notBeforeIn)))
                .claim("scope", scope).build();
        var jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256)
                .type(JOSEObjectType.JWT).keyID(key.getKeyID()).build(), claims);
        jwt.sign(new RSASSASigner(key));
        return jwt.serialize();
    }
}
