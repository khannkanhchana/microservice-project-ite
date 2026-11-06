package kh.edu.istad.platform.order.client.port.out.client;

import kh.edu.istad.platform.order.application.dto.QueryCustomerResult;
import reactor.core.publisher.Flux;

public interface CustomerServiceClient {
    Flux<QueryCustomerResult> getCustomerNames();





}
