package kh.edu.istad.platform.order;

import kh.edu.istad.platform.order.client.httpclient.BusinessHttpClient;
import kh.edu.istad.platform.order.client.port.out.client.CustomerServiceClient;
import kh.edu.istad.platform.order.config.ServicePropConfig;
import kh.edu.istad.platform.order.exception.OAuth2ExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;

import static org.mockito.Mockito.*;

class OrderAuthorizationFailureTests {
    @Test
    void bothOrderRoutesReturnSafe503WhenTokenAcquisitionFails() {
        var customers = mock(CustomerServiceClient.class);
        var businesses = mock(BusinessHttpClient.class);
        var failure = new OAuth2AuthorizationException(new OAuth2Error("server_error"),
                "Internal failure containing sensitive upstream details");
        when(customers.getCustomerNames()).thenReturn(Flux.error(failure));
        when(businesses.queryBusinesses()).thenReturn(Flux.error(failure));
        var controller = new WebFluxController(customers, businesses, mock(ServicePropConfig.class));
        var client = WebTestClient.bindToController(controller)
                .controllerAdvice(new OAuth2ExceptionHandler()).build();

        for (String route : new String[]{"/customers", "/businesses"}) {
            client.get().uri(route).exchange().expectStatus().isEqualTo(503)
                    .expectBody()
                    .jsonPath("$.status").isEqualTo(503)
                    .jsonPath("$.title").isEqualTo("Service authorization unavailable")
                    .jsonPath("$.detail").isEqualTo("Unable to obtain a service access token. Try again later.")
                    .jsonPath("$.stackTrace").doesNotExist();
        }
    }
}
