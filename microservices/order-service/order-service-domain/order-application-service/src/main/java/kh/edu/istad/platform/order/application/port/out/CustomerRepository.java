package kh.edu.istad.platform.order.application.port.out;



import kh.edu.istad.platform.order.application.dto.CustomerSyncCommand;
import reactor.core.publisher.Mono;

public interface CustomerRepository {

    Mono<Void> upsert(CustomerSyncCommand command);

}

