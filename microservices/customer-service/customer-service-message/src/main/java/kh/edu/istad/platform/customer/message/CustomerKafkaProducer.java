package kh.edu.istad.platform.customer.message;


import kh.edu.istad.platform.customer.domain.event.CustomerInitiatedEvent;
import kh.edu.istad.platform.customer.domain.port.out.CustomerEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class CustomerKafkaProducer implements CustomerEventPublisher {

    private static final String TOPIC = "customer-events";

    private final KafkaTemplate<String, CustomerInitiatedMessage> kafkaTemplate;

    @Override
    public Mono<Void> publish(CustomerInitiatedEvent event) {

        var customer = event.getCustomer();

        CustomerInitiatedMessage message = new CustomerInitiatedMessage(
                customer.getId().id(),
                customer.getUsername(),
                customer.getFamilyName(),
                customer.getGivenName(),
                customer.getEmail().email(),
                customer.getPhoneNumber().phoneNumber(),
                customer.getStatus().name(),
                event.getInitiatedAt()
        );

        return Mono.fromFuture(
                kafkaTemplate.send(
                        TOPIC,
                        customer.getId().id().toString(),
                        message
                )
        ).then();
    }
}