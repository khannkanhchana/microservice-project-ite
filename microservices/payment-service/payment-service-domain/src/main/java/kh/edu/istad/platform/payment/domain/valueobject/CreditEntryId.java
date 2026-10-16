package kh.edu.istad.platform.payment.domain.valueobject;

import kh.edu.istad.platform.payment.domain.exception.DomainException;

import java.util.UUID;

public record CreditEntryId(UUID value) {
    public CreditEntryId { if (value == null) throw new DomainException("CreditEntryId is required"); }
    public static CreditEntryId generate() { return new CreditEntryId(UUID.randomUUID()); }
}
