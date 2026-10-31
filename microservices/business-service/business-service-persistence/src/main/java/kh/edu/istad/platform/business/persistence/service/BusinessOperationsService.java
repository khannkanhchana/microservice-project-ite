package kh.edu.istad.platform.business.persistence.service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;
import com.mongodb.MongoException;
import kh.edu.istad.platform.business.application.port.out.BusinessOperations;
import kh.edu.istad.platform.business.domain.entity.*;
import kh.edu.istad.platform.business.domain.valueobject.*;
import kh.edu.istad.platform.business.persistence.document.BusinessDocument;
import kh.edu.istad.platform.business.persistence.document.ProductDocument;
import kh.edu.istad.platform.business.persistence.mapper.BusinessPersistenceMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

/** Reserves inventory and records its decision in one transaction, keyed by the immutable order ID. */
@Service
public class BusinessOperationsService implements BusinessOperations {
    private final ReactiveMongoTemplate mongo;
    private final BusinessPersistenceMapper mapper;
    private final TransactionalOperator transactions;
    private final WebClient orders;

    public BusinessOperationsService(ReactiveMongoTemplate mongo, BusinessPersistenceMapper mapper,
            TransactionalOperator transactions, @Qualifier("businessDiscoveryClient") WebClient.Builder builder) {
        this.mongo = mongo;
        this.mapper = mapper;
        this.transactions = transactions;
        orders = builder.clone().baseUrl("http://order-service").build();
    }

    @Document("order_approvals")
    public record ApprovalRecord(@Id String id, String businessId, String status, List<String> failureMessages) {}
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    public record OrderSnapshot(UUID orderId, UUID businessId, String status, BigDecimal totalAmount, List<OrderItem> items) {}
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    public record OrderItem(UUID productId, String name, BigDecimal price, int quantity) {}

    @Override
    public reactor.core.publisher.Flux<View> list() {
        return mongo.findAll(BusinessDocument.class).map(this::view);
    }

    @Override
    public Mono<View> setActive(UUID businessId, boolean active) {
        return mongo.findAndModify(Query.query(Criteria.where("_id").is(businessId.toString())),
                Update.update("active", active), org.springframework.data.mongodb.core.FindAndModifyOptions.options().returnNew(true),
                BusinessDocument.class)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Business not found")))
                .map(this::view);
    }

    @Override
    public Mono<View> get(UUID id) {
        return find(id).map(this::view);
    }

    private Mono<BusinessDocument> find(UUID id) {
        return mongo.findById(id.toString(), BusinessDocument.class)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Business not found")));
    }

    private View view(BusinessDocument business) {
        return new View(UUID.fromString(business.id()), business.legalName(), business.displayName(), business.active(),
                business.products().stream().map(p -> new CatalogProduct(UUID.fromString(p.id()), p.name(), p.price(), p.quantity(), p.available())).toList());
    }

    @Override
    public Mono<View> putProduct(UUID businessId, UUID productId, CatalogProduct product) {
        // Validate through domain value objects before storing API input.
        new Product(new ProductId(productId), product.name(), new Money(product.price()), product.quantity(), product.available());
        return atomic(Mono.defer(() -> find(businessId).flatMap(business -> {
            List<ProductDocument> products = new ArrayList<>(business.products());
            products.removeIf(p -> p.id().equals(productId.toString()));
            products.add(new ProductDocument(productId.toString(), product.name(), product.price(), product.quantity(), product.available()));
            return mongo.save(new BusinessDocument(business.id(), business.legalName(), business.displayName(),
                    business.email(), business.phoneNumber(), business.active(), products)).map(this::view);
        })));
    }

    @Override
    public Mono<Approval> approve(UUID businessId, UUID orderId) {
        // Check the authoritative order, rather than accepting a caller's claimed PAID status or total.
        return orders.get().uri("/api/orders/{id}", orderId).retrieve().bodyToMono(OrderSnapshot.class)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Order service returned an empty response")))
                .flatMap(order -> {
                    if (!orderId.equals(order.orderId()) || order.businessId() == null || order.status() == null
                            || order.totalAmount() == null || order.items() == null || order.items().isEmpty()
                            || order.items().stream().anyMatch(item -> item == null || item.productId() == null
                                    || item.name() == null || item.price() == null || item.quantity() <= 0))
                        return Mono.error(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Order service returned incomplete order data"));
                    if (!businessId.equals(order.businessId()))
                        return Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, "Order belongs to another business"));
                    return atomic(Mono.defer(() -> mongo.findById(orderId.toString(), ApprovalRecord.class)
                            .map(record -> new Approval(record.status(), record.failureMessages()))
                            .switchIfEmpty(Mono.defer(() -> reserve(businessId, order)))));
                });
    }

    private Mono<Approval> reserve(UUID businessId, OrderSnapshot order) {
        if (!"PAID".equals(order.status())) {
            return Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, "Only paid orders can receive a new approval"));
        }
        return find(businessId).flatMap(document -> {
            var detail = new OrderDetail(new OrderId(order.orderId()), OrderStatus.valueOf(order.status()), new Money(order.totalAmount()),
                    order.items().stream().map(p -> new Product(new ProductId(p.productId()), p.name(), new Money(p.price()), p.quantity(), true)).toList());
            var decision = mapper.toDomain(document).approve(detail);
            Mono<?> stock = Mono.just(document);
            if (decision.status() == OrderApprovalStatus.APPROVED) {
                var quantities = new HashMap<String, Integer>();
                order.items().forEach(p -> quantities.put(p.productId().toString(), p.quantity()));
                var products = document.products().stream().map(p -> new ProductDocument(p.id(), p.name(), p.price(),
                        p.quantity() - quantities.getOrDefault(p.id(), 0), p.available())).toList();
                // Same Mongo transaction as the idempotency record; concurrent catalog writes cause a retry.
                stock = mongo.save(new BusinessDocument(document.id(), document.legalName(), document.displayName(),
                        document.email(), document.phoneNumber(), document.active(), products));
            }
            var approval = new ApprovalRecord(order.orderId().toString(), businessId.toString(), decision.status().name(), decision.failureMessages());
            return stock.then(mongo.insert(approval)).map(saved -> new Approval(saved.status(), saved.failureMessages()));
        });
    }

    private <T> Mono<T> atomic(Mono<T> action) {
        return transactions.transactional(action).retryWhen(Retry.backoff(5, Duration.ofMillis(50))
                .filter(this::transientFailure).onRetryExhaustedThrow((spec, signal) -> signal.failure()));
    }

    private boolean transientFailure(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof MongoException mongoError && mongoError.hasErrorLabel("TransientTransactionError")) return true;
            if (cause instanceof org.springframework.dao.DuplicateKeyException) return true;
        }
        return false;
    }
}
