package kh.edu.istad.platform.customer.domain.usecase;

import kh.edu.istad.common.domain.valueobject.CustomerId;
import kh.edu.istad.platform.customer.domain.dto.UpdateCustomerCommand;
import kh.edu.istad.platform.customer.domain.event.CustomerDeactivatedEvent;
import kh.edu.istad.platform.customer.domain.exception.CustomerNotFoundException;
import kh.edu.istad.platform.customer.domain.port.out.CustomerRepository;
import kh.edu.istad.platform.customer.domain.service.CustomerDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DeactivateCustomerUseCase {
    private final CustomerRepository customerRepository;
    private final CustomerDomainService customerDomainService;

    public Mono<CustomerDeactivatedEvent> execute(UUID customerId) {
        return Mono.defer(() -> customerRepository.findById(new CustomerId(customerId)))
                .switchIfEmpty(Mono.error(() -> new CustomerNotFoundException(customerId)))
                .flatMap(customer -> {
                    var event = customerDomainService.deactivateCustomer(customer);
                    return customerRepository.save(customer).thenReturn(event);
                });
    }
}