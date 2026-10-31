package kh.edu.istad.platform.customer.domain.usecase;

import java.util.UUID;
import kh.edu.istad.common.domain.valueobject.CustomerId;
import kh.edu.istad.platform.customer.domain.entity.Customer;
import kh.edu.istad.platform.customer.domain.port.out.CustomerRepository;
import kh.edu.istad.platform.customer.domain.exception.CustomerNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GetCustomerUseCase {
    private final CustomerRepository repository;

    public Customer execute(UUID id) {
        return repository.findById(new CustomerId(id))
                .orElseThrow(() -> new CustomerNotFoundException(id));
    }
}
