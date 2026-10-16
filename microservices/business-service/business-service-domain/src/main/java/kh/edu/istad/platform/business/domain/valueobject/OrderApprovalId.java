package kh.edu.istad.platform.business.domain.valueobject;

import kh.edu.istad.platform.business.domain.exception.DomainException;

import java.util.UUID;

public record OrderApprovalId(UUID value) {
    public OrderApprovalId { if (value == null) throw new DomainException("OrderApprovalId is required"); }
    public static OrderApprovalId generate() { return new OrderApprovalId(UUID.randomUUID()); }
}
