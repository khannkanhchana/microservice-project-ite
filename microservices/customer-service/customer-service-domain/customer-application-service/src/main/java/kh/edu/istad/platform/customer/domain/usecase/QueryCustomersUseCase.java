package kh.edu.istad.platform.customer.domain.usecase;

import kh.edu.istad.platform.customer.domain.entity.Customer;
import kh.edu.istad.platform.customer.domain.port.out.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
@RequiredArgsConstructor
public class QueryCustomersUseCase {
    private final CustomerRepository customerRepository;

    public Flux<Customer> execute(String username, String email, String phoneNumber) {
        return customerRepository.query(username, email, phoneNumber);
    }
}
