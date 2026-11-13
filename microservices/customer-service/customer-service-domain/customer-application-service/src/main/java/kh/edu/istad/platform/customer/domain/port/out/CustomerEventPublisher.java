package kh.edu.istad.platform.customer.domain.port.out;


import kh.edu.istad.platform.customer.domain.event.CustomerInitiatedEvent;
import reactor.core.publisher.Mono;

public interface CustomerEventPublisher {

    Mono<Void> publish(CustomerInitiatedEvent event);

}