
package kh.edu.istad.platform.payment.persistence;



import kh.edu.istad.platform.payment.domain.entity.CreditEntry;
import kh.edu.istad.platform.payment.domain.entity.Payment;
import kh.edu.istad.platform.payment.domain.service.PaymentOutcome;
import kh.edu.istad.platform.payment.domain.valueobject.CustomerId;
import kh.edu.istad.platform.payment.domain.valueobject.Money;
import kh.edu.istad.platform.payment.domain.valueobject.OrderId;
import kh.edu.istad.platform.payment.domain.valueobject.PaymentStatus;
import kh.edu.istad.platform.payment.persistence.dto.OrderResponse;
import kh.edu.istad.platform.payment.persistence.repository.CreditEntryRepository;
import kh.edu.istad.platform.payment.persistence.repository.PaymentRepository;
import kh.edu.istad.platform.payment.usecase.ProcessPaymentUseCase;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.UUID;

@Service
public class PaymentPersistenceService {

    private final WebClient orderServiceClient;
    private final PaymentRepository paymentRepository;
    private final CreditEntryRepository creditEntryRepository;
    private final ProcessPaymentUseCase processPaymentUseCase;
    private final Clock paymentClock;

    public PaymentPersistenceService(
            @Qualifier("loadBalancedWebClientBuilder")
            WebClient.Builder webClientBuilder,
            PaymentRepository paymentRepository,
            CreditEntryRepository creditEntryRepository,
            ProcessPaymentUseCase processPaymentUseCase,
            Clock paymentClock
    ) {
        this.orderServiceClient =
                webClientBuilder.baseUrl("http://order-service").build();
        this.paymentRepository = paymentRepository;
        this.creditEntryRepository = creditEntryRepository;
        this.processPaymentUseCase = processPaymentUseCase;
        this.paymentClock = paymentClock;
    }

    public Mono<OrderResponse> getOrder(UUID orderId) {
        return orderServiceClient.get()
                .uri("/api/orders/{orderId}", orderId)
                .retrieve()
                .bodyToMono(OrderResponse.class)
                .switchIfEmpty(Mono.error(
                        new IllegalStateException("Order not found")
                ));
    }

    public Mono<PaymentOutcome> processPayment(
            UUID orderId,
            UUID customerId,
            BigDecimal amount
    ) {
        return getOrder(orderId)
                .flatMap(order -> {
                    if (order.totalAmount().compareTo(amount) != 0) {
                        return Mono.error(
                                new IllegalArgumentException(
                                        "Payment amount must match order total"
                                )
                        );
                    }

                    return paymentRepository.findByOrderId(orderId)
                            .hasElement()
                            .flatMap(exists -> {
                                if (exists) {
                                    return Mono.error(
                                            new IllegalStateException(
                                                    "A payment already exists for this order"
                                            )
                                    );
                                }

                                return creditEntryRepository
                                        .findByCustomerId(customerId)
                                        .switchIfEmpty(Mono.error(
                                                new IllegalStateException(
                                                        "Credit account not found"
                                                )
                                        ))
                                        .flatMap(credit -> processAndSave(
                                                orderId,
                                                customerId,
                                                amount,
                                                credit
                                        ));
                            });
                });
    }

    private Mono<PaymentOutcome> processAndSave(
            UUID orderId,
            UUID customerId,
            BigDecimal amount,
            CreditEntry credit
    ) {
        Payment payment = Payment.initialize(
                new OrderId(orderId),
                new CustomerId(customerId),
                new Money(amount),
                ZonedDateTime.now(paymentClock)
        );

        PaymentOutcome outcome =
                processPaymentUseCase.execute(payment, credit);

        if (outcome.payment().status() == PaymentStatus.FAILED) {
            return paymentRepository.save(outcome.payment())
                    .thenReturn(outcome);
        }

        return paymentRepository.save(outcome.payment())
                .then(creditEntryRepository.save(outcome.creditEntry()))
                .thenReturn(outcome);
    }
}
