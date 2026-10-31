package kh.edu.istad.platform.customer.domain.port.out;

import kh.edu.istad.common.domain.valueobject.CustomerId;
import kh.edu.istad.platform.customer.domain.entity.Customer;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

public interface CustomerRepository {
    Mono<Customer> save(Customer customer);
    Mono<Customer> findById(CustomerId customerId);
    Flux<Customer> findAll();
    Flux<Customer> query(String username, String email, String phoneNumber);
}
