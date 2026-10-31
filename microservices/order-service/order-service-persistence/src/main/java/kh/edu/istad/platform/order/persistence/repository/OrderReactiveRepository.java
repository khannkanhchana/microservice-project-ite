package kh.edu.istad.platform.order.persistence.repository;

import kh.edu.istad.platform.order.persistence.entity.OrderEntity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface OrderReactiveRepository extends R2dbcRepository<OrderEntity, UUID> {
    Flux<OrderEntity> findAllBy(Pageable pageable);
}
