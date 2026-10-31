package kh.edu.istad.platform.order.application.port.out;


import kh.edu.istad.platform.order.domain.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;


public interface OrderRepository {


    Page<Order> findAll(Pageable pageable);

    Order save (Order order);

    Optional<Order> findById(UUID orderId);

    Order update(UUID orderId, java.util.function.UnaryOperator<Order> transition);
}
