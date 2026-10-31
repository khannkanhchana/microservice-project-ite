package kh.edu.istad.platform.payment.persistence;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import com.mongodb.MongoException;
import kh.edu.istad.platform.payment.domain.entity.*;
import kh.edu.istad.platform.payment.domain.service.PaymentOutcome;
import kh.edu.istad.platform.payment.domain.service.PaymentDomainService;
import kh.edu.istad.platform.payment.domain.valueobject.*;
import kh.edu.istad.platform.payment.persistence.dto.OrderResponse;
import kh.edu.istad.platform.payment.persistence.document.CreditEntryDocument;
import kh.edu.istad.platform.payment.persistence.repository.*;
import kh.edu.istad.platform.payment.usecase.ProcessPaymentUseCase;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

/** Coordinates local payment transactions; HTTP dependency calls stay outside those transactions. */
@Service
public class PaymentPersistenceService {
    private final WebClient orders;
    private final WebClient customers;
    private final PaymentRepository payments;
    private final CreditEntryRepository credits;
    private final ProcessPaymentUseCase process;
    private final Clock clock;
    private final ReactiveMongoTemplate mongo;
    private final TransactionalOperator transactions;

    public PaymentPersistenceService(@Qualifier("loadBalancedWebClientBuilder") WebClient.Builder builder,
            PaymentRepository payments, CreditEntryRepository credits, ProcessPaymentUseCase process,
            Clock clock, ReactiveMongoTemplate mongo, TransactionalOperator transactions) {
        orders = builder.clone().baseUrl("http://order-service").build();
        customers = builder.clone().baseUrl("http://customer-service").build();
        this.payments = payments;
        this.credits = credits;
        this.process = process;
        this.clock = clock;
        this.mongo = mongo;
        this.transactions = transactions;
    }

    public Mono<OrderResponse> getOrder(UUID id) {
        return orders.get().uri("/api/orders/{id}", id).retrieve().bodyToMono(OrderResponse.class)
                .switchIfEmpty(Mono.error(problem(HttpStatus.SERVICE_UNAVAILABLE, "Order service returned an empty response")))
                .flatMap(order -> {
                    if (!id.equals(order.id()) || order.customerId() == null || order.totalAmount() == null || order.status() == null)
                        return Mono.error(problem(HttpStatus.SERVICE_UNAVAILABLE, "Order service returned incomplete order data"));
                    return Mono.just(order);
                });
    }

    public Mono<PaymentOutcome> processPayment(UUID orderId, UUID customerId, BigDecimal amount) {
        return getOrder(orderId).flatMap(order -> {
            if (!customerId.equals(order.customerId())) return Mono.error(problem(HttpStatus.CONFLICT, "Customer does not own this order"));
            if (amount.compareTo(order.totalAmount()) != 0) return Mono.error(problem(HttpStatus.BAD_REQUEST, "Payment amount must match order total"));
            return payments.findByOrderId(orderId).flatMap(payment -> existingOutcome(payment, customerId, amount))
                    .switchIfEmpty(Mono.defer(() -> requireActiveCustomer(customerId).then(charge(order, customerId, amount))));
        });
    }

    private Mono<PaymentOutcome> charge(OrderResponse order, UUID customerId, BigDecimal amount) {
        // Recheck under the transaction: another instance may have charged after the preflight read.
        return atomic(Mono.defer(() -> payments.findByOrderId(order.id())
                .flatMap(payment -> existingOutcome(payment, customerId, amount))
                .switchIfEmpty(Mono.defer(() -> createPayment(order, customerId, amount)))));
    }

    private Mono<PaymentOutcome> createPayment(OrderResponse order, UUID customerId, BigDecimal amount) {
        if (!"PENDING".equals(order.status())) return Mono.error(problem(HttpStatus.CONFLICT, "Only pending orders can be paid"));
        return credits.findByCustomerId(customerId)
                .switchIfEmpty(Mono.error(problem(HttpStatus.NOT_FOUND, "Credit account not found")))
                .flatMap(credit -> {
                    var payment = Payment.initialize(new OrderId(order.id()), new CustomerId(customerId),
                            new Money(amount), ZonedDateTime.now(clock));
                    var result = process.execute(payment, credit);
                    Mono<?> debit = result.payment().status() == PaymentStatus.COMPLETED
                            ? credits.save(result.creditEntry()) : Mono.just(credit);
                    return debit.then(payments.save(result.payment())).thenReturn(result);
                });
    }

    private Mono<PaymentOutcome> existingOutcome(Payment payment, UUID customerId, BigDecimal amount) {
        if (!payment.customerId().value().equals(customerId) || payment.price().amount().compareTo(amount) != 0)
            return Mono.error(problem(HttpStatus.CONFLICT, "Existing payment does not match the requested customer and amount"));
        return outcome(payment);
    }

    private Mono<PaymentOutcome> outcome(Payment payment) {
        return credits.findByCustomerId(payment.customerId().value())
                .switchIfEmpty(Mono.error(problem(HttpStatus.NOT_FOUND, "Credit account not found")))
                .map(credit -> new PaymentOutcome(payment, credit, payment.status() == PaymentStatus.FAILED
                        ? Optional.of("Insufficient credit") : Optional.empty()));
    }

    public Mono<PaymentOutcome> payment(UUID paymentId) {
        return payments.findById(paymentId)
                .switchIfEmpty(Mono.error(problem(HttpStatus.NOT_FOUND, "Payment not found")))
                .flatMap(this::outcome);
    }

    public Mono<PaymentOutcome> paymentForOrder(UUID orderId) {
        return payments.findByOrderId(orderId)
                .switchIfEmpty(Mono.error(problem(HttpStatus.NOT_FOUND, "Payment not found")))
                .flatMap(this::outcome);
    }

    public Mono<PaymentOutcome> refund(UUID orderId) {
        return getOrder(orderId).flatMap(order -> {
            if (!Set.of("CANCELLING", "CANCELLED").contains(order.status()))
                return Mono.error(problem(HttpStatus.CONFLICT, "Order must be cancelling before refund"));
            return atomic(Mono.defer(() -> payments.findByOrderId(orderId)
                    .switchIfEmpty(Mono.error(problem(HttpStatus.NOT_FOUND, "Payment not found")))
                    .flatMap(payment -> {
                        if (payment.status() == PaymentStatus.CANCELLED) return outcome(payment);
                        if (payment.status() != PaymentStatus.COMPLETED)
                            return Mono.error(problem(HttpStatus.CONFLICT, "Only completed payments can be refunded"));
                        return credits.findByCustomerId(payment.customerId().value())
                                .switchIfEmpty(Mono.error(problem(HttpStatus.NOT_FOUND, "Credit account not found")))
                                .flatMap(credit -> {
                            var result = new PaymentDomainService().cancel(payment, credit, ZonedDateTime.now(clock));
                            return credits.save(result.creditEntry()).then(payments.save(result.payment())).thenReturn(result);
                        });
                    })));
        });
    }

    public record Account(UUID customerId, BigDecimal balance) {}
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    public record CustomerSnapshot(String status) {}

    private Mono<Void> requireActiveCustomer(UUID customerId) {
        return customers.get().uri("/api/customers/{id}", customerId).retrieve().bodyToMono(CustomerSnapshot.class)
                .switchIfEmpty(Mono.error(problem(HttpStatus.SERVICE_UNAVAILABLE, "Customer service returned an empty response")))
                .flatMap(customer -> "ACTIVE".equals(customer.status()) ? Mono.<Void>empty()
                        : Mono.error(problem(HttpStatus.CONFLICT, "Customer is inactive")));
    }

    public Mono<Account> openAccount(UUID customerId, BigDecimal initialBalance) {
        new Money(initialBalance);
        return requireActiveCustomer(customerId).then(Mono.defer(() -> {
                    var document = new CreditEntryDocument();
                    document.setId(customerId.toString());
                    document.setCustomerId(customerId);
                    document.setTotalCreditAmount(initialBalance);
                    return mongo.insert(document).map(saved -> new Account(customerId, saved.getTotalCreditAmount()));
                })).onErrorMap(org.springframework.dao.DuplicateKeyException.class,
                        error -> problem(HttpStatus.CONFLICT, "Credit account already exists"));
    }

    public Mono<Account> account(UUID customerId) {
        return credits.findByCustomerId(customerId).map(credit -> new Account(customerId, credit.totalCreditAmount().amount()))
                .switchIfEmpty(Mono.error(problem(HttpStatus.NOT_FOUND, "Credit account not found")));
    }

    private <T> Mono<T> atomic(Mono<T> action) {
        return transactions.transactional(action).retryWhen(Retry.backoff(5, Duration.ofMillis(50))
                .filter(this::transientFailure).onRetryExhaustedThrow((spec, signal) -> signal.failure()));
    }

    private boolean transientFailure(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof MongoException mongoError && mongoError.hasErrorLabel("TransientTransactionError")) return true;
            if (cause instanceof org.springframework.dao.DuplicateKeyException
                    || cause instanceof org.springframework.dao.OptimisticLockingFailureException) return true;
        }
        return false;
    }

    private ResponseStatusException problem(HttpStatus status, String message) {
        return new ResponseStatusException(status, message);
    }
}
