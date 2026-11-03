package kh.edu.istad.platform.order.client.adapter;


import kh.edu.istad.platform.order.application.dto.QueryCustomerResult;
import kh.edu.istad.platform.order.client.httpclient.CustomerHttpClient;
import kh.edu.istad.platform.order.client.port.out.client.CustomerServiceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

@Component
@RequiredArgsConstructor
public class CustomerServiceClientAdapter implements CustomerServiceClient {

    private final WebClient.Builder webClientBuilder;
    private final CustomerHttpClient customerHttpClient;

//    public record GivenName(String givenName){
//
//    }

    @Override
    public Flux<QueryCustomerResult> getCustomerNames() {
//        WebClient webClient = webClientBuilder
//                .baseUrl("http://customer-service")
//                .build();
//
//        return webClient
//                .get()
//                .uri("/api/customers")
//                .retrieve()
//                .bodyToFlux(QueryCustomerResult.class);
        return customerHttpClient.queryCustomers();
    }
}
