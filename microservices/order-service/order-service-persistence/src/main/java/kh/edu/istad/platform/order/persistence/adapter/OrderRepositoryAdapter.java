package kh.edu.istad.platform.order.persistence.adapter;

import kh.edu.istad.platform.order.application.port.out.OrderRepository;
import kh.edu.istad.platform.order.domain.entity.Order;
import kh.edu.istad.platform.order.persistence.mapper.OrderPersistenceMapper;
import kh.edu.istad.platform.order.persistence.repository.OrderReactiveRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.transaction.annotation.Transactional;
import kh.edu.istad.platform.order.persistence.entity.OrderEntity;
import kh.edu.istad.platform.order.persistence.entity.OrderItemEmbeddable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class OrderRepositoryAdapter implements OrderRepository {

    private final OrderReactiveRepository repository;
    private final OrderPersistenceMapper mapper;
    private final R2dbcEntityTemplate template;
    private final DatabaseClient database;

    @Override
    public Flux<Order> findAll() {
        return repository.findAll()
                .concatMap(this::loadOrder);
    }

    @Override
    public Mono<Page<Order>> findAll(Pageable pageable) {
        return repository.findAllBy(pageable)
                .concatMap(this::loadOrder)
                .collectList()
                .zipWith(repository.count())
                .map(result -> new PageImpl<>(result.getT1(), pageable, result.getT2()));
    }

    @Override
    @Transactional
    public Mono<Order> save(Order order) {
        return Mono.defer(() -> {
            var entity = mapper.toEntity(order);
            return repository.existsById(entity.getId())
                    .flatMap(exists -> exists ? template.update(entity) : template.insert(entity))
                    .flatMap(saved -> database.sql("DELETE FROM order_items WHERE order_id = :orderId")
                            .bind("orderId", saved.getId()).fetch().rowsUpdated()
                            .thenMany(Flux.fromIterable(entity.getItems())
                                    .concatMap(item -> insertItem(saved.getId(), item)))
                            .then(Mono.just(order)));
        });
    }

    @Override
    public Mono<Order> findById(UUID orderId) {
        return repository.findById(orderId)
                .flatMap(this::loadOrder);
    }

    private Mono<Order> loadOrder(OrderEntity entity) {
        return database.sql("""
                SELECT id, product_name, quantity, price, sub_total
                FROM order_items WHERE order_id = :orderId ORDER BY id
                """)
                .bind("orderId", entity.getId())
                .map((row, metadata) -> {
                    var item = new OrderItemEmbeddable();
                    item.setItemId(row.get("id", Integer.class));
                    item.setProductName(row.get("product_name", String.class));
                    item.setQuantity(row.get("quantity", Integer.class));
                    item.setPrice(row.get("price", java.math.BigDecimal.class));
                    item.setSubTotal(row.get("sub_total", java.math.BigDecimal.class));
                    return item;
                }).all().collectList()
                .map(items -> {
                    entity.setItems(items);
                    return mapper.toDomain(entity);
                });
    }

    private Mono<Long> insertItem(UUID orderId, OrderItemEmbeddable item) {
        return database.sql("""
                INSERT INTO order_items
                    (id, order_id, product_name, quantity, price, sub_total)
                VALUES (:itemId, :orderId, :name, :quantity, :price, :subTotal)
                """)
                .bind("orderId", orderId)
                .bind("itemId", item.getItemId())
                .bind("name", item.getProductName())
                .bind("quantity", item.getQuantity())
                .bind("price", item.getPrice())
                .bind("subTotal", item.getSubTotal())
                .fetch().rowsUpdated();
    }
}
