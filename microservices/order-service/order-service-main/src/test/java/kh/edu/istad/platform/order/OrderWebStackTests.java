package kh.edu.istad.platform.order;

import kh.edu.istad.platform.order.client.config.OAuth2ClientConfig;
import kh.edu.istad.platform.order.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;

class OrderWebStackTests {
    @Test
    void reactiveSecurityStartsWithoutServletWebStack() {
        assertThat(ClassUtils.isPresent("org.springframework.web.servlet.DispatcherServlet",
                getClass().getClassLoader())).isFalse();

        new ReactiveWebApplicationContextRunner()
                .withUserConfiguration(SecurityConfig.class, OAuth2ClientConfig.class)
                .withBean(ReactiveClientRegistrationRepository.class, () ->
                        new InMemoryReactiveClientRegistrationRepository(
                                ClientRegistration.withRegistrationId("customer-service")
                                        .clientId("test-client").clientSecret("test-secret")
                                        .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                                        .tokenUri("http://localhost/oauth2/token").build()))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(SecurityWebFilterChain.class);
                    assertThat(context).hasSingleBean(ReactiveOAuth2AuthorizedClientService.class);
                });
    }
}
