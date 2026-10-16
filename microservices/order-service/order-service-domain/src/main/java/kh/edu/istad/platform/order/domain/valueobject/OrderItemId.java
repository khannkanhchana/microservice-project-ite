package kh.edu.istad.platform.order.domain.valueobject;

import kh.edu.istad.platform.order.domain.exception.DomainException;

import java.util.UUID;

public record OrderItemId(UUID value) {
    public OrderItemId { if (value == null) throw new DomainException("OrderItemId is required"); }
    public static OrderItemId generate() { return new OrderItemId(UUID.randomUUID()); }
}
