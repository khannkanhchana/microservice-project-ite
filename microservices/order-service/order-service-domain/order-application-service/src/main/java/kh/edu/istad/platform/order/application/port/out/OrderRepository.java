package kh.edu.istad.platform.order.application.port.out;


import kh.edu.istad.platform.order.domain.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.UUID;


public interface OrderRepository {


    Flux<Order> findAll();

    Mono<Page<Order>> findAll(Pageable pageable);

    Mono<Order> save(Order order);

    Mono<Order> findById(UUID orderId);
}
