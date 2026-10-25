
package kh.edu.istad.platform.payment.restapi.controller;

import jakarta.validation.Valid;
import kh.edu.istad.platform.payment.domain.entity.Payment;
import kh.edu.istad.platform.payment.domain.valueobject.CustomerId;
import kh.edu.istad.platform.payment.domain.valueobject.Money;
import kh.edu.istad.platform.payment.domain.valueobject.OrderId;
import kh.edu.istad.platform.payment.persistence.PaymentPersistenceService;
import kh.edu.istad.platform.payment.persistence.dto.OrderResponse;
import kh.edu.istad.platform.payment.restapi.dto.PaymentResponse;
import kh.edu.istad.platform.payment.restapi.dto.ProcessPaymentRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentPersistenceService paymentPersistenceService;
    private final Clock paymentClock;

    @GetMapping("/test-order/{orderId}")
    public OrderResponse testOrder(@PathVariable("orderId") UUID orderId) {
        return paymentPersistenceService.getOrder(orderId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse process(
            @Valid @RequestBody ProcessPaymentRequest request) {

        // Verify that the order exists in Order Service.
        paymentPersistenceService.getOrder(request.orderId());

        // Initialize payment.
        var customerId = new CustomerId(request.customerId());

        var payment = Payment.initialize(
                new OrderId(request.orderId()),
                customerId,
                new Money(request.amount()),
                ZonedDateTime.now(paymentClock)
        );

        /*
         * Payment processing must be delegated to your existing
         * ProcessPaymentUseCase and persistence implementation.
         *
         * Do not call paymentPersistenceService.process(...)
         * because that method does not exist in the code provided.
         */

        throw new UnsupportedOperationException(
                "Connect the existing ProcessPaymentUseCase and "
                        + "payment persistence implementation here."
        );
    }
}
