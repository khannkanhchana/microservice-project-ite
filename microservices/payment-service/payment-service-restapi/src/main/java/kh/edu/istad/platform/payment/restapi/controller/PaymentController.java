
package kh.edu.istad.platform.payment.restapi.controller;

import jakarta.validation.Valid;
import kh.edu.istad.platform.payment.persistence.PaymentPersistenceService;
import kh.edu.istad.platform.payment.persistence.dto.OrderResponse;
import kh.edu.istad.platform.payment.restapi.dto.PaymentResponse;
import kh.edu.istad.platform.payment.restapi.dto.ProcessPaymentRequest;
import kh.edu.istad.platform.payment.domain.service.PaymentOutcome;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentPersistenceService paymentPersistenceService;

    @GetMapping("/{paymentId}")
    public Mono<PaymentResponse> get(@PathVariable("paymentId") UUID paymentId) {
        return paymentPersistenceService.payment(paymentId).map(this::response);
    }

    @GetMapping("/orders/{orderId}")
    public Mono<PaymentResponse> getForOrder(@PathVariable("orderId") UUID orderId) {
        return paymentPersistenceService.paymentForOrder(orderId).map(this::response);
    }

    @PostMapping("/orders/{orderId}/refund")
    public Mono<PaymentResponse> refund(@PathVariable("orderId") UUID orderId) {
        return paymentPersistenceService.refund(orderId).map(this::response);
    }

    @GetMapping("/test-order/{orderId}")
    public Mono<OrderResponse> testOrder(
            @PathVariable("orderId") UUID orderId
    ) {
        return paymentPersistenceService.getOrder(orderId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<PaymentResponse> process(
            @Valid @RequestBody ProcessPaymentRequest request
    ) {
        return paymentPersistenceService.processPayment(
                request.orderId(),
                request.customerId(),
                request.amount()
        ).map(this::response).onErrorMap(
                IllegalArgumentException.class,
                ex -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        ex.getMessage(),
                        ex
                )
        ).onErrorMap(
                IllegalStateException.class,
                ex -> {
                    String message = ex.getMessage();

                    if (message != null
                            && message.contains("not found")) {
                        return new ResponseStatusException(
                                HttpStatus.NOT_FOUND, message, ex
                        );
                    }

                    return new ResponseStatusException(
                            HttpStatus.CONFLICT, message, ex
                    );
                }
        );
    }

    private PaymentResponse response(PaymentOutcome outcome) {
        return new PaymentResponse(outcome.payment().id().value(), outcome.payment().orderId().value(),
                outcome.payment().customerId().value(), outcome.payment().status().name(),
                outcome.payment().price().amount(), outcome.creditEntry().totalCreditAmount().amount(),
                outcome.failureMessage().map(List::of).orElseGet(List::of));
    }
}
