package kh.edu.istad.platform.order.persistence.adapter;

import kh.edu.istad.platform.order.application.port.out.OrderRepository;
import kh.edu.istad.platform.order.domain.entity.Order;
import kh.edu.istad.platform.order.persistence.mapper.OrderPersistenceMapper;
import kh.edu.istad.platform.order.persistence.repository.OrderJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;


@Repository
@RequiredArgsConstructor
public class OrderRepositoryAdapter implements OrderRepository {

    private final OrderJpaRepository jpaRepository;
    private final OrderPersistenceMapper orderPersistenceMapper;

    @Override
    public Page<Order> findAll(Pageable pageable) {
        return jpaRepository.findAll(pageable).map(orderPersistenceMapper::toDomain);
    }

    @Override
    @Transactional
    public Order save(Order order) {
        var entity = orderPersistenceMapper.toEntity(order);
        jpaRepository.saveAndFlush(entity);
        return order;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Order> findById(UUID orderId) {
        return jpaRepository.findById(orderId)
                .map(orderPersistenceMapper::toDomain);
    }


}
