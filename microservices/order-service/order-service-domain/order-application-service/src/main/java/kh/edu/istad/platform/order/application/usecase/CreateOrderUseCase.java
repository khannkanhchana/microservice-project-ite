package kh.edu.istad.platform.order.application.usecase;

import kh.edu.istad.platform.order.application.dto.CreateOrderCommand;
import kh.edu.istad.platform.order.application.dto.CreateOrderResult;
import kh.edu.istad.platform.order.application.dto.Item;
import kh.edu.istad.platform.order.application.dto.QueryProductResult;
import kh.edu.istad.platform.order.application.port.out.OrderRepository;
import kh.edu.istad.platform.order.application.port.out.client.ProductServiceClient;
import kh.edu.istad.platform.order.domain.entity.Order;
import kh.edu.istad.platform.order.domain.entity.OrderItem;
import kh.edu.istad.platform.order.domain.entity.Product;
import kh.edu.istad.platform.order.domain.exception.DomainException;
import kh.edu.istad.platform.order.domain.valueobject.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreateOrderUseCase {

    private final OrderRepository orderRepository;
    private final ProductServiceClient productServiceClient;

    public Mono<CreateOrderResult> execute(CreateOrderCommand command) {

        // ── 1. Guard: validate command ────────────────────────────────────────
        if (command == null) {
            return Mono.error(new DomainException("Order command is required"));
        }
        if (command.items() == null || command.items().isEmpty()) {
            return Mono.error(new DomainException("At least one order item is required"));
        }
        if (command.items().stream().anyMatch(item -> item == null)) {
            return Mono.error(new DomainException("Order items cannot contain null"));
        }

        // ── 2. Validate every product via gRPC and fetch authoritative price ──
        return Flux.fromIterable(command.items())
                .flatMap(item -> productServiceClient
                        .getProductById(item.productId().toString())
                        .doOnNext(result -> log.info(
                                "Product validated via gRPC: id={}, name={}, price={}",
                                result.id(), result.name(), result.price()
                        ))
                        .map(result -> buildOrderItem(item, result))
                )
                .collectList()
                .flatMap(orderItems -> {

                    // ── 3. Build domain aggregates ────────────────────────────
                    var orderId = OrderId.generate();

                    var address = new StreetAddress(
                            UUID.randomUUID(),
                            command.street(),
                            command.postalCode(),
                            command.city()
                    );

                    // Re-index order item IDs after reactive collection
                    List<OrderItem> indexedItems = new java.util.ArrayList<>();
                    for (int i = 0; i < orderItems.size(); i++) {
                        var raw = orderItems.get(i);
                        indexedItems.add(OrderItem.create(
                                new OrderItemId(i + 1),
                                orderId,
                                raw.product(),
                                raw.quantity()
                        ));
                    }

                    var order = Order.initializeOrder(
                            orderId,
                            new CustomerId(command.customerId()),
                            new BusinessId(command.businessId()),
                            address,
                            indexedItems
                    );

                    // ── 4. Persist and return result ──────────────────────────
                    return orderRepository.save(order)
                            .map(saved -> new CreateOrderResult(
                                    saved.orderId().id(),
                                    saved.trackingId().id(),
                                    saved.status(),
                                    saved.price().amount()
                            ));
                });
    }

    /**
     * Constructs a temporary {@link OrderItem} shell using the authoritative
     * price fetched from product-service via gRPC. The item IDs are
     * re-indexed after all gRPC calls complete.
     */
    private OrderItem buildOrderItem(Item item, QueryProductResult grpcProduct) {
        // Use the real price from product-service, NOT the client-supplied price
        BigDecimal authorizedPrice = BigDecimal.valueOf(grpcProduct.price())
                .setScale(2, java.math.RoundingMode.HALF_UP);

        var product = new Product(
                grpcProduct.name(),
                new Money(authorizedPrice)
        );

        // Placeholder ID=0; will be re-indexed in the flatMap above
        return OrderItem.create(
                new OrderItemId(0),
                OrderId.generate(),   // temporary; replaced during re-index
                product,
                item.quantity()
        );
    }
}
