package kh.edu.istad.platform.payment.domain.valueobject;

import kh.edu.istad.platform.payment.domain.exception.DomainException;

import java.util.UUID;

public record OrderId(UUID value) {
    public OrderId { if (value == null) throw new DomainException("OrderId is required"); }
    public static OrderId generate() { return new OrderId(UUID.randomUUID()); }
}
