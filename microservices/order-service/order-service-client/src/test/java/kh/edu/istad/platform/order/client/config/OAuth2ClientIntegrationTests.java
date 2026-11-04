package kh.edu.istad.platform.order.client.config;

import kh.edu.istad.platform.order.client.httpclient.BusinessHttpClient;
import kh.edu.istad.platform.order.client.httpclient.CustomerHttpClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

// HTTP dependency simulation. Real JWT validation is covered by resource-server tests.
class OAuth2ClientIntegrationTests {
    private DisposableServer authorizationServer;
    private DisposableServer resourceServer;
    private final AtomicInteger tokenRequests = new AtomicInteger();
    private final List<String> bearerHeaders = new CopyOnWriteArrayList<>();
    private final List<String> requestedScopes = new CopyOnWriteArrayList<>();
    private final List<String> clientAuthentication = new CopyOnWriteArrayList<>();
    private volatile int tokenLifetime = 3600;
    private volatile boolean authorizationUnavailable;
    private CustomerHttpClient customers;
    private BusinessHttpClient businesses;

    @BeforeEach
    void setUp() {
        authorizationServer = HttpServer.create().host("127.0.0.1").port(0)
                .route(routes -> routes.post("/oauth2/token", (request, response) -> {
                    int sequence = tokenRequests.incrementAndGet();
                    clientAuthentication.add(request.requestHeaders().get("Authorization", ""));
                    return request.receive().aggregate().asString().flatMap(form -> {
                        requestedScopes.add(form);
                        if (authorizationUnavailable) {
                            return response.status(503).header("Content-Type", "application/json")
                                    .sendString(Mono.just("{\"error\":\"temporarily_unavailable\"}")).then();
                        }
                        return response.header("Content-Type", "application/json")
                                .sendString(Mono.just("{\"access_token\":\"test-token-" + sequence
                                        + "\",\"token_type\":\"Bearer\",\"expires_in\":" + tokenLifetime + "}"))
                                .then();
                    });
                })).bindNow();
        resourceServer = HttpServer.create().host("127.0.0.1").port(0)
                .handle((request, response) -> {
                    String header = request.requestHeaders().get("Authorization", "");
                    bearerHeaders.add(header);
                    if (!header.startsWith("Bearer test-token-")) {
                        return response.status(401).send();
                    }
                    return response.header("Content-Type", "application/json").sendString(Mono.just("[]"));
                }).bindNow();
        var registrations = new InMemoryReactiveClientRegistrationRepository(
                registration("customer-service", "customer.read"),
                registration("business-service", "business.read"));
        var config = new OAuth2ClientConfig();
        var service = config.reactiveAuthorizedClientService(registrations);
        var manager = config.authorizedClientManager(registrations, service);
        customers = proxy(CustomerHttpClient.class, config.customerOAuth2Filter(manager, service));
        businesses = proxy(BusinessHttpClient.class, config.businessOAuth2Filter(manager, service));
    }

    @AfterEach
    void stopServers() {
        if (authorizationServer != null) authorizationServer.disposeNow();
        if (resourceServer != null) resourceServer.disposeNow();
    }

    @Test
    void customerClientAcquiresAttachesAndReusesToken() {
        StepVerifier.create(customers.queryCustomers()).verifyComplete();
        StepVerifier.create(customers.queryCustomers()).verifyComplete();
        assertThat(tokenRequests.get()).isEqualTo(1);
        assertThat(bearerHeaders).hasSize(2).allMatch(header -> header.equals("Bearer test-token-1"));
        assertThat(requestedScopes).allMatch(form -> form.contains("scope=customer.read")
                && form.contains("grant_type=client_credentials"));
        assertThat(clientAuthentication).allMatch(header -> header.startsWith("Basic "));
    }

    @Test
    void businessClientAcquiresAndPropagatesItsOwnScopedToken() {
        StepVerifier.create(businesses.queryBusinesses()).verifyComplete();
        assertThat(requestedScopes).hasSize(1);
        assertThat(requestedScopes.getFirst()).contains("scope=business.read");
        assertThat(bearerHeaders).containsExactly("Bearer test-token-1");
    }

    @Test
    void registrationsKeepCustomerAndBusinessTokensSeparate() {
        StepVerifier.create(customers.queryCustomers()).verifyComplete();
        StepVerifier.create(businesses.queryBusinesses()).verifyComplete();
        assertThat(tokenRequests.get()).isEqualTo(2);
        assertThat(bearerHeaders).containsExactly("Bearer test-token-1", "Bearer test-token-2");
    }

    @Test
    void renewsTokenInsideExpiryWindowWithoutWaiting() {
        tokenLifetime = 1;
        StepVerifier.create(customers.queryCustomers()).verifyComplete();
        StepVerifier.create(customers.queryCustomers()).verifyComplete();
        assertThat(tokenRequests.get()).isGreaterThanOrEqualTo(2);
        assertThat(bearerHeaders).hasSize(2);
        assertThat(bearerHeaders.get(1)).isNotEqualTo(bearerHeaders.get(0));
    }

    @Test
    void coldClientFailsClosedWhenAuthorizationServerUnavailable() {
        authorizationUnavailable = true;
        StepVerifier.create(customers.queryCustomers())
                .expectError(OAuth2AuthorizationException.class).verify(Duration.ofSeconds(10));
        StepVerifier.create(businesses.queryBusinesses())
                .expectError(OAuth2AuthorizationException.class).verify(Duration.ofSeconds(10));
        assertThat(bearerHeaders).isEmpty();
    }

    @Test
    void connectionFailureDoesNotReachResourceServer() {
        authorizationServer.disposeNow();
        StepVerifier.create(customers.queryCustomers())
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(OAuth2AuthorizationException.class);
                    assertThat(((OAuth2AuthorizationException) error).getError().getErrorCode())
                            .isEqualTo("temporarily_unavailable");
                }).verify(Duration.ofSeconds(10));
        StepVerifier.create(businesses.queryBusinesses())
                .expectError(OAuth2AuthorizationException.class).verify(Duration.ofSeconds(10));
        assertThat(bearerHeaders).isEmpty();
    }

    @Test
    void cachedTokenWorksDuringOutage() {
        StepVerifier.create(customers.queryCustomers()).verifyComplete();
        authorizationUnavailable = true;
        StepVerifier.create(customers.queryCustomers()).verifyComplete();
        assertThat(tokenRequests.get()).isEqualTo(1);
        assertThat(bearerHeaders).hasSize(2);
    }

    @Test
    void renewalFailsClosedDuringOutage() {
        tokenLifetime = 1;
        StepVerifier.create(customers.queryCustomers()).verifyComplete();
        int successfulRequests = bearerHeaders.size();
        authorizationUnavailable = true;
        StepVerifier.create(customers.queryCustomers())
                .expectError(OAuth2AuthorizationException.class).verify(Duration.ofSeconds(10));
        assertThat(bearerHeaders).hasSize(successfulRequests);
    }

    private ClientRegistration registration(String id, String scope) {
        return ClientRegistration.withRegistrationId(id)
                .clientId(id).clientSecret("test-secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .tokenUri("http://127.0.0.1:" + authorizationServer.port() + "/oauth2/token")
                .scope(scope).build();
    }

    private <T> T proxy(Class<T> type, ServerOAuth2AuthorizedClientExchangeFilterFunction filter) {
        var webClient = WebClient.builder()
                .baseUrl("http://127.0.0.1:" + resourceServer.port())
                .filter(filter).build();
        return HttpServiceProxyFactory.builderFor(WebClientAdapter.create(webClient))
                .build().createClient(type);
    }
}
