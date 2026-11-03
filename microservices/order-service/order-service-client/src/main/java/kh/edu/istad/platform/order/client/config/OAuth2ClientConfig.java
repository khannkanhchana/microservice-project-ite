package kh.edu.istad.platform.order.client.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.InMemoryReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.RemoveAuthorizedClientReactiveOAuth2AuthorizationFailureHandler;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;

@Configuration
public class OAuth2ClientConfig {
    @Bean
    ReactiveOAuth2AuthorizedClientService authorizedClientService(
            ReactiveClientRegistrationRepository registrations) {
        return new InMemoryReactiveOAuth2AuthorizedClientService(registrations);
    }

    @Bean
    ReactiveOAuth2AuthorizedClientManager authorizedClientManager(
            ReactiveClientRegistrationRepository registrations,
            ReactiveOAuth2AuthorizedClientService authorizedClients) {
        var manager = new AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(
                registrations, authorizedClients);
        manager.setAuthorizedClientProvider(ReactiveOAuth2AuthorizedClientProviderBuilder.builder()
                .clientCredentials()
                .build());
        return manager;
    }

    @Bean
    ServerOAuth2AuthorizedClientExchangeFilterFunction customerOAuth2Filter(
            ReactiveOAuth2AuthorizedClientManager authorizedClientManager,
            ReactiveOAuth2AuthorizedClientService authorizedClients) {
        return oauth2Filter("customer-service", authorizedClientManager, authorizedClients);
    }

    @Bean
    ServerOAuth2AuthorizedClientExchangeFilterFunction businessOAuth2Filter(
            ReactiveOAuth2AuthorizedClientManager authorizedClientManager,
            ReactiveOAuth2AuthorizedClientService authorizedClients) {
        return oauth2Filter("business-service", authorizedClientManager, authorizedClients);
    }

    private ServerOAuth2AuthorizedClientExchangeFilterFunction oauth2Filter(
            String registrationId, ReactiveOAuth2AuthorizedClientManager authorizedClientManager,
            ReactiveOAuth2AuthorizedClientService authorizedClients) {
        var filter = new ServerOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager);
        filter.setDefaultClientRegistrationId(registrationId);
        filter.setAuthorizationFailureHandler(
                new RemoveAuthorizedClientReactiveOAuth2AuthorizationFailureHandler(
                        (failedRegistrationId, principal, attributes) ->
                                authorizedClients.removeAuthorizedClient(failedRegistrationId, principal.getName())));
        return filter;
    }
}
