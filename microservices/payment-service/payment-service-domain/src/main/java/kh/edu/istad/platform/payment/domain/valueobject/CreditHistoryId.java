package kh.edu.istad.platform.payment.domain.valueobject;

import kh.edu.istad.platform.payment.domain.exception.DomainException;

import java.util.UUID;

public record CreditHistoryId(UUID value) {
    public CreditHistoryId { if (value == null) throw new DomainException("CreditHistoryId is required"); }
    public static CreditHistoryId generate() { return new CreditHistoryId(UUID.randomUUID()); }
}
