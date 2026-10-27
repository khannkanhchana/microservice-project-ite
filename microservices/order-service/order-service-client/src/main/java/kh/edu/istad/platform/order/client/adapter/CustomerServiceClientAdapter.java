package kh.edu.istad.platform.order.client.adapter;


import kh.edu.istad.platform.order.client.port.out.client.CustomerServiceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

@Component
@RequiredArgsConstructor
public class CustomerServiceClientAdapter implements CustomerServiceClient {

    private final WebClient.Builder webClientBuilder;

    public record GivenName(String givenName){

    }

    @Override
    public Flux<String> getCustomerNames() {
        WebClient webClient = webClientBuilder
                .baseUrl("http://customer-service")
                .build();

        var response = webClient
                .get()
                .uri("/api/customers")
                .retrieve()
                .bodyToFlux(GivenName.class);
        return response.map(GivenName::givenName);
    }
}