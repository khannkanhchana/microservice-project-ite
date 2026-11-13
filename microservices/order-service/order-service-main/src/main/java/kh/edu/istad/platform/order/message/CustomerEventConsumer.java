//
//
//=============Testing================
//package kh.edu.istad.platform.order.message;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import kh.edu.istad.platform.order.message.dto.CustomerInitiatedMessage;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.kafka.annotation.KafkaListener;
//import org.springframework.stereotype.Component;
//
//@Component
//public class CustomerEventConsumer {
//
//    private static final Logger log =
//            LoggerFactory.getLogger(CustomerEventConsumer.class);
//
//    private final ObjectMapper objectMapper;
//
//    public CustomerEventConsumer(ObjectMapper objectMapper) {
//        this.objectMapper = objectMapper;
//    }
//
//    @KafkaListener(
//            topics = "customer-events",
//            groupId = "${spring.kafka.consumer.group-id}"
//    )
//    public void consume(String message) {
//
//        try {
//            CustomerInitiatedMessage event =
//                    objectMapper.readValue(
//                            message,
//                            CustomerInitiatedMessage.class
//                    );
//
//            log.info("========== CUSTOMER EVENT RECEIVED ==========");
//            log.info("Customer ID: {}", event.customerId());
//            log.info("Username: {}", event.username());
//            log.info("Full Name: {} {}", event.givenName(), event.familyName());
//            log.info("Email: {}", event.email());
//            log.info("Phone: {}", event.phoneNumber());
//            log.info("Status: {}", event.status());
//            log.info("Initiated At: {}", event.initiatedAt());
//            log.info("==============================================");
//
//        } catch (Exception exception) {
//            log.error("Failed to process Customer Event: {}", message, exception);
//        }
//    }
//}



package kh.edu.istad.platform.order.message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kh.edu.istad.platform.order.application.dto.CustomerSyncCommand;
import kh.edu.istad.platform.order.application.usecase.SyncCustomerUseCase;
import kh.edu.istad.platform.order.message.dto.CustomerInitiatedMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomerEventConsumer {

    private final ObjectMapper objectMapper;
    private final SyncCustomerUseCase syncCustomerUseCase;

    @KafkaListener(
            topics = "customer-events",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(String message) {

        log.info("========== CUSTOMER EVENT RECEIVED ==========");

        try {

            CustomerInitiatedMessage event =
                    objectMapper.readValue(
                            message,
                            CustomerInitiatedMessage.class
                    );

            var command = new CustomerSyncCommand(
                    event.customerId(),
                    event.username(),
                    event.familyName(),
                    event.givenName()
            );

            syncCustomerUseCase.execute(command).block();

            log.info("Customer ID: {}", event.customerId());
            log.info("Username: {}", event.username());
            log.info("Full Name: {} {}", event.givenName(), event.familyName());
            log.info("Customer synchronized into Order Database");

            log.info("==============================================");

        } catch (JsonProcessingException exception) {

            log.error("Failed to deserialize Customer Event: {}", message, exception);

            throw new IllegalArgumentException(
                    "Invalid Customer Event JSON",
                    exception
            );
        }
    }
}
