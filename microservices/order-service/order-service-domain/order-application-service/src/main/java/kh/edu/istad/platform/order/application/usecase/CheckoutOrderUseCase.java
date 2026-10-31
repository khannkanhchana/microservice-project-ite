package kh.edu.istad.platform.order.application.usecase;

import java.util.List;
import java.util.UUID;
import kh.edu.istad.platform.order.application.port.out.CommerceGateway;
import kh.edu.istad.platform.order.application.port.out.OrderRepository;
import kh.edu.istad.platform.order.domain.entity.Order;
import kh.edu.istad.platform.order.domain.valueobject.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** A resumable synchronous saga. Remote writes are idempotent; state changes use short local locks. */
@Service
@RequiredArgsConstructor
public class CheckoutOrderUseCase {
    private final OrderRepository orders;
    private final CommerceGateway commerce;

    public Order execute(UUID id) {
        Order order = orders.findById(id).orElseThrow(() -> new CheckoutException(
                CheckoutException.Reason.NOT_FOUND, "Order not found"));
        if (order.status() == OrderStatus.APPROVED || order.status() == OrderStatus.CANCELLED) return order;
        if (order.status() == OrderStatus.CANCELLING) {
            commerce.refund(id);
            return cancel(id, order.failureMessages());
        }
        // Payment checks activity before a fresh charge. A committed charge stays replayable
        // even if the customer profile changes during recovery from a lost response.
        var payment = commerce.charge(id, order.customerId().id(), order.price().amount());
        if ("FAILED".equals(payment.status()) || "CANCELLED".equals(payment.status())) {
            var reasons = payment.failureMessages() == null || payment.failureMessages().isEmpty()
                    ? List.of("Payment was declined or refunded") : payment.failureMessages();
            return cancel(id, reasons);
        }
        if (!"COMPLETED".equals(payment.status()))
            throw new CheckoutException(CheckoutException.Reason.CONFLICT, "Payment has not completed");
        order = orders.update(id, current -> current.status() == OrderStatus.PENDING ? current.pay() : current);
        if (order.status() == OrderStatus.APPROVED || order.status() == OrderStatus.CANCELLED) return order;
        var approval = commerce.approve(order.businessId().id(), id);
        if ("APPROVED".equals(approval.status())) {
            return orders.update(id, current -> current.status() == OrderStatus.PAID ? current.approve() : current);
        }
        if (!"REJECTED".equals(approval.status())) {
            throw new CheckoutException(CheckoutException.Reason.UNAVAILABLE, "Business returned an unknown approval outcome");
        }
        // Persist compensation intent before refunding. Retrying checkout safely repeats the refund.
        orders.update(id, current -> current.status() == OrderStatus.PAID
                ? current.initCancel(approval.failureMessages()) : current);
        commerce.refund(id);
        return cancel(id, approval.failureMessages());
    }

    private Order cancel(UUID id, List<String> reasons) {
        return orders.update(id, current -> switch (current.status()) {
            case PENDING, CANCELLING -> current.cancel(reasons);
            case PAID -> current.initCancel(reasons).cancel(reasons);
            default -> current;
        });
    }
}
