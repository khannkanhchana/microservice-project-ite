package kh.edu.istad.platform.order.persistence.adapter;



import kh.edu.istad.platform.order.application.dto.CustomerSyncCommand;
import kh.edu.istad.platform.order.application.port.out.CustomerRepository;
import kh.edu.istad.platform.order.persistence.entity.CustomerEntity;
import kh.edu.istad.platform.order.persistence.repository.CustomerReactiveRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
public class CustomerRepositoryAdapter implements CustomerRepository {

    private final CustomerReactiveRepository repository;
    private final R2dbcEntityTemplate template;

    @Override
    public Mono<Void> upsert(CustomerSyncCommand command) {

        return repository.findById(command.customerId())

                .flatMap(existing -> {
                    existing.setUsername(command.username());
                    existing.setFamilyName(command.familyName());
                    existing.setGivenName(command.givenName());

                    return template.update(existing);
                })

                .switchIfEmpty(Mono.defer(() ->
                        template.insert(toEntity(command))
                ))

                .then();
    }

    private CustomerEntity toEntity(CustomerSyncCommand command) {

        var entity = new CustomerEntity();

        entity.setId(command.customerId());
        entity.setUsername(command.username());
        entity.setFamilyName(command.familyName());
        entity.setGivenName(command.givenName());

        return entity;
    }
}
