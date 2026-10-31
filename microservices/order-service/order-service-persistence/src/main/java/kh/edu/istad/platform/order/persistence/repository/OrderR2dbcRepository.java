//package kh.edu.istad.platform.order.persistence.repository;
//
//import kh.edu.istad.platform.order.persistence.entity.OrderEntity;
//import org.springframework.data.repository.reactive.ReactiveCrudRepository;
//import org.springframework.data.domain.Pageable;
//import reactor.core.publisher.Flux;
//
//import java.util.UUID;
//
//public interface OrderR2dbcRepository extends ReactiveCrudRepository<OrderEntity, UUID> {
//    Flux<OrderEntity> findAllBy(Pageable pageable);
//}
