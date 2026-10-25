
package kh.edu.istad.platform.payment.persistence;





import kh.edu.istad.platform.payment.persistence.dto.OrderResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.UUID;

@Service
public class PaymentPersistenceService {

    private final RestClient orderServiceClient;

    public PaymentPersistenceService(
            @Qualifier("loadBalancedRestClientBuilder")
            RestClient.Builder builder) {

        this.orderServiceClient = builder
                .baseUrl("http://order-service")
                .build();
    }

    /**
     * Calls Order Service using Eureka service discovery
     * and Spring Cloud LoadBalancer.
     */
    public OrderResponse getOrder(UUID orderId) {
        if (orderId == null) {
            throw new IllegalArgumentException("Order ID is required");
        }

        try {
            OrderResponse order = orderServiceClient.get()
                    .uri("/api/orders/{orderId}", orderId)
                    .retrieve()
                    .body(OrderResponse.class);

            if (order == null) {
                throw new IllegalStateException(
                        "Order Service returned an empty response");
            }

            return order;

        } catch (RestClientResponseException ex) {
            throw new IllegalStateException(
                    "Failed to retrieve order " + orderId
                            + ". Order Service returned HTTP "
                            + ex.getStatusCode().value(),
                    ex);
        }
    }
}
