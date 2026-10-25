package kh.edu.istad.platform.payment.restapi.client;




import kh.edu.istad.platform.payment.restapi.dto.OrderResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.UUID;

@Component
public class OrderClient {

    private final RestClient restClient;

    public OrderClient() {
        this.restClient = RestClient.builder()
                .baseUrl("http://order-service")
                .build();
    }

    public OrderResponse getOrder(UUID orderId) {
        if (orderId == null) {
            throw new IllegalArgumentException("Order ID is required");
        }

        try {
            OrderResponse order = restClient.get()
                    .uri("/api/orders/{orderId}", orderId)
                    .retrieve()
                    .body(OrderResponse.class);

            if (order == null) {
                throw new IllegalStateException(
                        "Order Service returned an empty response"
                );
            }

            return order;
        } catch (RestClientResponseException ex) {
            throw new IllegalStateException(
                    "Failed to retrieve order " + orderId
                            + ". HTTP status: "
                            + ex.getStatusCode().value(),
                    ex
            );
        }
    }
}
