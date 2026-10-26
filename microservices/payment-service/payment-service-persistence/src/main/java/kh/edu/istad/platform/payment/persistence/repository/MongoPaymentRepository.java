package kh.edu.istad.platform.payment.persistence.repository;


import kh.edu.istad.platform.payment.domain.entity.Payment;
import kh.edu.istad.platform.payment.domain.valueobject.CustomerId;
import kh.edu.istad.platform.payment.domain.valueobject.Money;
import kh.edu.istad.platform.payment.domain.valueobject.OrderId;
import kh.edu.istad.platform.payment.domain.valueobject.PaymentId;
import kh.edu.istad.platform.payment.domain.valueobject.PaymentStatus;
import kh.edu.istad.platform.payment.persistence.document.PaymentDocument;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;

@Repository
public class MongoPaymentRepository implements PaymentRepository {

    private final ReactiveMongoTemplate mongoTemplate;

    public MongoPaymentRepository(ReactiveMongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Mono<Payment> save(Payment payment) {
        PaymentDocument document = toDocument(payment);

        return mongoTemplate.save(document)
                .map(this::toDomain);
    }

    @Override
    public Mono<Payment> findById(UUID paymentId) {
        if (paymentId == null) {
            return Mono.error(
                    new IllegalArgumentException("Payment ID is required")
            );
        }

        return mongoTemplate.findById(
                        paymentId.toString(),
                        PaymentDocument.class
                )
                .map(this::toDomain);
    }

    @Override
    public Mono<Payment> findByOrderId(UUID orderId) {
        if (orderId == null) {
            return Mono.error(
                    new IllegalArgumentException("Order ID is required")
            );
        }

        Query query = Query.query(
                Criteria.where("orderId").is(orderId)
        );

        return mongoTemplate.findOne(query, PaymentDocument.class)
                .map(this::toDomain);
    }

    private PaymentDocument toDocument(Payment payment) {
        PaymentDocument document = new PaymentDocument();

        document.setId(payment.id().value().toString());
        document.setOrderId(payment.orderId().value());
        document.setCustomerId(payment.customerId().value());
        document.setAmount(payment.price().amount());
        document.setStatus(payment.status().name());
        document.setCreatedAt(payment.createdAt().toInstant());

        return document;
    }

    private Payment toDomain(PaymentDocument document) {
        return new Payment(
                new PaymentId(UUID.fromString(document.getId())),
                new OrderId(document.getOrderId()),
                new CustomerId(document.getCustomerId()),
                new Money(document.getAmount()),
                PaymentStatus.valueOf(document.getStatus()),
                ZonedDateTime.ofInstant(
                        document.getCreatedAt(),
                        ZoneOffset.UTC
                )
        );
    }
}
