package kh.edu.istad.platform.order.application.port.out.client;

import kh.edu.istad.platform.order.application.dto.QueryCustomerResult;
import reactor.core.publisher.Flux;

public interface CustomerServiceClient {

    Flux<QueryCustomerResult> getCustomers();

    Flux<QueryCustomerResult> getCustomerById(String id);

    Flux<QueryCustomerResult> createCustomer(QueryCustomerResult result);

}
