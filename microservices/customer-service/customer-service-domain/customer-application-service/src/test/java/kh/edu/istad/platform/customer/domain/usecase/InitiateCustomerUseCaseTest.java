package kh.edu.istad.platform.customer.domain.usecase;

import kh.edu.istad.platform.customer.domain.dto.InitiateCustomerCommand;
import kh.edu.istad.platform.customer.domain.entity.Customer;
import kh.edu.istad.platform.customer.domain.port.out.CustomerRepository;
import kh.edu.istad.platform.customer.domain.service.CustomerDomainServiceImpl;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class InitiateCustomerUseCaseTest {
    private final InitiateCustomerCommand command =
            new InitiateCustomerCommand("chana", "Chan", "Na", "c@example.com", "012345678");

    @Test
    void creationIsDeferredAndReturnsPersistedCustomer() {
        CustomerRepository repository = mock(CustomerRepository.class);
        when(repository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0, Customer.class)));
        var useCase = new InitiateCustomerUseCase(new CustomerDomainServiceImpl(), repository);
        var result = useCase.execute(command);
        verifyNoInteractions(repository);
        StepVerifier.create(result).assertNext(created -> {
            assertNotNull(created.customerId());
            assertEquals("chana", created.username());
            assertEquals("c@example.com", created.email());
        }).verifyComplete();
        verify(repository).save(any());
    }

    @Test
    void persistenceFailurePropagatesAsReactiveError() {
        CustomerRepository repository = mock(CustomerRepository.class);
        when(repository.save(any())).thenReturn(Mono.error(new IllegalStateException("Database unavailable")));
        var useCase = new InitiateCustomerUseCase(new CustomerDomainServiceImpl(), repository);
        StepVerifier.create(useCase.execute(command))
                .expectErrorMessage("Database unavailable").verify();
    }
}