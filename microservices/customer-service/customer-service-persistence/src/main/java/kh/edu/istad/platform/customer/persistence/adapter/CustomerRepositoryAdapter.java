package kh.edu.istad.platform.customer.persistence.adapter;

import kh.edu.istad.common.domain.valueobject.CustomerId;
import kh.edu.istad.platform.customer.domain.entity.Customer;
import kh.edu.istad.platform.customer.domain.port.out.CustomerRepository;
import kh.edu.istad.platform.customer.persistence.entity.CustomerEntity;
import kh.edu.istad.platform.customer.persistence.mapper.CustomerPersistenceMapper;
import kh.edu.istad.platform.customer.persistence.repository.CustomerReactiveRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

@Repository
@RequiredArgsConstructor
public class CustomerRepositoryAdapter implements CustomerRepository {
    private final CustomerReactiveRepository customerReactiveRepository;
    private final CustomerPersistenceMapper customerPersistenceMapper;
    private final R2dbcEntityTemplate template;

    @Override
    public Mono<Customer> save(Customer customer) {
        return Mono.defer(() -> {
            CustomerEntity entity = customerPersistenceMapper.toEntity(customer);
            // Domain IDs are assigned before persistence; explicitly insert new customers.
            return customerReactiveRepository.existsById(entity.getCustomerId())
                    .flatMap(exists -> exists
                            ? template.update(entity)
                            : template.insert(entity))
                    .map(customerPersistenceMapper::toDomain);
        });
    }

    @Override
    public Mono<Customer> findById(CustomerId customerId) {
        return customerReactiveRepository.findById(customerId.id())
                .map(customerPersistenceMapper::toDomain);
    }

    @Override
    public Flux<Customer> findAll() {
        return customerReactiveRepository.findAll()
                .map(customerPersistenceMapper::toDomain);
    }

    @Override
    public Flux<Customer> query(String username, String email, String phoneNumber) {
        Criteria criteria = Criteria.empty();
        if (username != null && !username.isBlank()) {
            criteria = criteria.and("username").is(username);
        }
        if (email != null && !email.isBlank()) {
            criteria = criteria.and("email").is(email);
        }
        if (phoneNumber != null && !phoneNumber.isBlank()) {
            criteria = criteria.and("phoneNumber").is(phoneNumber);
        }
        return template.select(Query.query(criteria), CustomerEntity.class)
                .map(customerPersistenceMapper::toDomain);
    }
}
