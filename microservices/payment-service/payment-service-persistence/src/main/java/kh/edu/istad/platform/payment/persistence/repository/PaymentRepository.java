package kh.edu.istad.platform.payment.persistence.repository;

import kh.edu.istad.platform.payment.domain.entity.Payment;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface PaymentRepository {

    Mono<Payment> save(Payment payment);

    Mono<Payment> findById(UUID paymentId);

    Mono<Payment> findByOrderId(UUID orderId);
}
