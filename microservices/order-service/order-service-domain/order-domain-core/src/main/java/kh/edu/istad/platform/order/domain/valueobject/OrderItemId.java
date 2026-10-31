package kh.edu.istad.platform.order.domain.valueobject;

import kh.edu.istad.platform.order.domain.exception.DomainException;

public record OrderItemId(Integer id) {
    public OrderItemId {
        if (id == null) {
            throw new DomainException("Order item ID is required");
        }
        if (id <= 0) {
            throw new DomainException("Order item ID must be positive");
        }
    }

}
