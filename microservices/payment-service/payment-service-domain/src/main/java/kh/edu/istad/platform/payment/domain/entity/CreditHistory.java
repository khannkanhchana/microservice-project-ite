package kh.edu.istad.platform.payment.domain.entity;

import kh.edu.istad.platform.payment.domain.exception.DomainException;
import kh.edu.istad.platform.payment.domain.valueobject.CreditHistoryId;
import kh.edu.istad.platform.payment.domain.valueobject.CustomerId;
import kh.edu.istad.platform.payment.domain.valueobject.Money;
import kh.edu.istad.platform.payment.domain.valueobject.TransactionType;

public record CreditHistory(
        CreditHistoryId id,
        CustomerId customerId,
        Money amount,
        TransactionType transactionType) {
    public CreditHistory {
        if (id == null || customerId == null || amount == null || transactionType == null) throw new DomainException("Invalid credit history");
        amount.requirePositive();
    }
}
