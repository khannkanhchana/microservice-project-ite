package kh.edu.istad.platform.customer.persistence.adapter;

import kh.edu.istad.common.domain.valueobject.CustomerId;
import kh.edu.istad.platform.customer.domain.entity.Customer;
import kh.edu.istad.platform.customer.persistence.entity.CustomerEntity;
import kh.edu.istad.platform.customer.persistence.mapper.CustomerPersistenceMapper;
import kh.edu.istad.platform.customer.persistence.repository.CustomerReactiveRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Query;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CustomerRepositoryAdapterTest {
    private final CustomerReactiveRepository repository = mock(CustomerReactiveRepository.class);
    private final CustomerPersistenceMapper mapper = mock(CustomerPersistenceMapper.class);
    private final R2dbcEntityTemplate template = mock(R2dbcEntityTemplate.class);
    private final CustomerRepositoryAdapter adapter = new CustomerRepositoryAdapter(repository, mapper, template);

    @Test
    void assignedUuidIsInsertedWhenCustomerDoesNotExist() {
        UUID id = UUID.randomUUID();
        Customer customer = Customer.Builder.builder().id(new CustomerId(id)).build();
        CustomerEntity entity = new CustomerEntity();
        entity.setCustomerId(id);
        when(mapper.toEntity(customer)).thenReturn(entity);
        when(repository.existsById(id)).thenReturn(Mono.just(false));
        when(template.insert(entity)).thenReturn(Mono.just(entity));
        when(mapper.toDomain(entity)).thenReturn(customer);

        StepVerifier.create(adapter.save(customer)).expectNext(customer).verifyComplete();
        verify(template).insert(entity);
        verify(template, never()).update(any(CustomerEntity.class));
    }

    @Test
    void queryStreamsCustomersWithoutPaginationOrCount() {
        CustomerEntity entity = new CustomerEntity();
        Customer customer = Customer.Builder.builder().build();
        when(template.select(any(Query.class), eq(CustomerEntity.class))).thenReturn(Flux.just(entity));
        when(mapper.toDomain(entity)).thenReturn(customer);
        StepVerifier.create(adapter.query(null, null, null)).expectNext(customer).verifyComplete();
        verify(template).select(argThat(query -> query.getLimit() <= 0 && query.getOffset() <= 0),
                eq(CustomerEntity.class));
        verify(template, never()).count(any(Query.class), eq(CustomerEntity.class));
    }

    @Test
    void queryBindsFiltersWithoutConcatenatingSql() {
        when(template.select(any(Query.class), eq(CustomerEntity.class))).thenReturn(Flux.empty());
        StepVerifier.create(adapter.query("chana", null, "012345678")).verifyComplete();
        verify(template).select(argThat(query -> {
            String criteria = query.getCriteria().orElseThrow().toString();
            return criteria.contains("username") && criteria.contains("phoneNumber")
                    && !criteria.contains("email");
        }), eq(CustomerEntity.class));
    }
}
