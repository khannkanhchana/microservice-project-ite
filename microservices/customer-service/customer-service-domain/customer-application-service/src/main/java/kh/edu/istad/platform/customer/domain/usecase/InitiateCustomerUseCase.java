package kh.edu.istad.platform.customer.domain.usecase;

import kh.edu.istad.platform.customer.domain.dto.InitiateCustomerCommand;
import kh.edu.istad.platform.customer.domain.dto.InitiateCustomerResult;
import kh.edu.istad.platform.customer.domain.entity.Customer;
import kh.edu.istad.platform.customer.domain.port.out.CustomerRepository;
import kh.edu.istad.platform.customer.domain.service.CustomerDomainService;
import kh.edu.istad.platform.customer.domain.valueobject.Email;
import kh.edu.istad.platform.customer.domain.valueobject.PhoneNumber;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class InitiateCustomerUseCase {
    private final CustomerDomainService customerDomainService;
    private final CustomerRepository customerRepository;

    public Mono<InitiateCustomerResult> execute(InitiateCustomerCommand command) {
        return Mono.defer(() -> {
            Customer customer = Customer.Builder.builder()
                    .username(command.username())
                    .familyName(command.familyName())
                    .givenName(command.givenName())
                    .email(new Email(command.email()))
                    .phoneNumber(new PhoneNumber(command.phoneNumber()))
                    .build();
            customerDomainService.initiateCustomer(customer);
            return customerRepository.save(customer);
        }).map(customer -> new InitiateCustomerResult(
                customer.getId().id(), customer.getUsername(), customer.getFamilyName(),
                customer.getGivenName(), customer.getEmail().email(),
                customer.getPhoneNumber().phoneNumber()));
    }
}