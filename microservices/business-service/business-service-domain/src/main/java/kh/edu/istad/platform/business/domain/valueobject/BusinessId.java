package kh.edu.istad.platform.business.domain.valueobject;

import kh.edu.istad.platform.business.domain.exception.DomainException;

import java.util.UUID;

public record BusinessId(UUID value) {
    public BusinessId { if (value == null) throw new DomainException("BusinessId is required"); }
    public static BusinessId generate() { return new BusinessId(UUID.randomUUID()); }
}
