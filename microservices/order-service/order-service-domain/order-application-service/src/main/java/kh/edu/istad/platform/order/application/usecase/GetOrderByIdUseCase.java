package kh.edu.istad.platform.order.application.usecase;



import kh.edu.istad.platform.order.application.port.out.OrderRepository;
import kh.edu.istad.platform.order.domain.entity.Order;
import kh.edu.istad.platform.order.domain.exception.DomainException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetOrderByIdUseCase {

    private final OrderRepository orderRepository;

    @Transactional(readOnly = true)
    public Order execute(UUID orderId) {
        if (orderId == null) {
            throw new DomainException("Order ID is required");
        }

        return orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new DomainException("Order not found: " + orderId));
    }
}
