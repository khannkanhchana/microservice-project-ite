package kh.edu.istad.platform.customer.domain.usecase;

import kh.edu.istad.common.domain.valueobject.CustomerId;
import kh.edu.istad.platform.customer.domain.dto.UpdateCustomerCommand;
import kh.edu.istad.platform.customer.domain.event.CustomerUpdatedEvent;
import kh.edu.istad.platform.customer.domain.exception.CustomerNotFoundException;
import kh.edu.istad.platform.customer.domain.port.out.CustomerRepository;
import kh.edu.istad.platform.customer.domain.service.CustomerDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UpdateCustomerUseCase {
    private final CustomerRepository customerRepository;
    private final CustomerDomainService customerDomainService;

    public Mono<CustomerUpdatedEvent> execute(UpdateCustomerCommand command) {
        return Mono.defer(() -> customerRepository.findById(new CustomerId(command.customerId())))
                .switchIfEmpty(Mono.error(() -> new CustomerNotFoundException(command.customerId())))
                .flatMap(customer -> {
                    var event = customerDomainService.updateCustomer(customer, command.familyName(), command.givenName());
                    return customerRepository.save(customer).thenReturn(event);
                });
    }
}