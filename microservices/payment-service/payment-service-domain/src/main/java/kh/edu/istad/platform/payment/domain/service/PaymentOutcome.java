package kh.edu.istad.platform.payment.domain.service;


import kh.edu.istad.platform.payment.domain.entity.CreditEntry;
import kh.edu.istad.platform.payment.domain.entity.Payment;

import java.util.Optional;

public record PaymentOutcome(
        Payment payment,
        CreditEntry creditEntry,
        Optional<String> failureMessage
) {
    public PaymentOutcome {
        if (payment == null || creditEntry == null) {
            throw new IllegalArgumentException(
                    "Payment and credit entry are required"
            );
        }

        failureMessage = failureMessage == null
                ? Optional.empty()
                : failureMessage;
    }
}
