package kh.edu.istad.platform.order.application.usecase;


import kh.edu.istad.platform.order.application.dto.CustomerSyncCommand;
import kh.edu.istad.platform.order.application.port.out.CustomerRepository;
import kh.edu.istad.platform.order.domain.exception.DomainException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class SyncCustomerUseCase {

    private final CustomerRepository customerRepository;

    public Mono<Void> execute(CustomerSyncCommand command) {

        if (command == null) {
            return Mono.error(
                    new DomainException("Customer command is required")
            );
        }

        if (command.customerId() == null) {
            return Mono.error(
                    new DomainException("Customer ID is required")
            );
        }

        return customerRepository.upsert(command)
                .doOnSuccess(unused ->
                        log.info(
                                "Customer synchronized successfully: {}",
                                command.customerId()
                        )
                );
    }
}
