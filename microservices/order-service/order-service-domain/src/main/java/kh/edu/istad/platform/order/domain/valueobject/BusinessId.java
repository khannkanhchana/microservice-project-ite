package kh.edu.istad.platform.order.domain.valueobject;

import kh.edu.istad.platform.order.domain.exception.DomainException;

import java.util.UUID;

public record BusinessId(UUID value) {
    public BusinessId { if (value == null) throw new DomainException("BusinessId is required"); }
    public static BusinessId generate() { return new BusinessId(UUID.randomUUID()); }
}
