package kh.edu.istad.platform.order.domain.valueobject;

import kh.edu.istad.platform.order.domain.exception.DomainException;

import java.util.UUID;

public record CustomerId(UUID value) {
    public CustomerId { if (value == null) throw new DomainException("CustomerId is required"); }
    public static CustomerId generate() { return new CustomerId(UUID.randomUUID()); }
}
