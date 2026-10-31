package kh.edu.istad.platform.payment.domain.service;


import kh.edu.istad.platform.payment.domain.entity.CreditEntry;
import kh.edu.istad.platform.payment.domain.entity.Payment;
import kh.edu.istad.platform.payment.domain.exception.DomainException;
import kh.edu.istad.platform.payment.domain.valueobject.PaymentStatus;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;

public class PaymentDomainService {

    public PaymentOutcome process(
            Payment payment,
            CreditEntry credit,
            ZonedDateTime now
    ) {
        Objects.requireNonNull(payment, "Payment is required");
        Objects.requireNonNull(credit, "Credit account is required");
        Objects.requireNonNull(now, "Processing time is required");

        if (!payment.customerId().equals(credit.customerId())) {
            throw new DomainException(
                    "Payment customer does not match credit account"
            );
        }

        if (payment.status() != PaymentStatus.PENDING) {
            throw new DomainException(
                    "Only pending payments can be processed"
            );
        }

        if (!credit.totalCreditAmount().covers(payment.price())) {
            return new PaymentOutcome(
                    payment.fail(),
                    credit,
                    Optional.of("Insufficient credit")
            );
        }

        CreditEntry updatedCredit =
                credit.subtractCreditAmount(payment.price());

        Payment completedPayment = payment.complete();

        return new PaymentOutcome(
                completedPayment,
                updatedCredit,
                Optional.empty()
        );
    }

    public PaymentOutcome cancel(
            Payment payment,
            CreditEntry credit,
            ZonedDateTime now
    ) {
        Objects.requireNonNull(payment, "Payment is required");
        Objects.requireNonNull(credit, "Credit account is required");
        Objects.requireNonNull(now, "Cancellation time is required");

        if (!payment.customerId().equals(credit.customerId())) {
            throw new DomainException(
                    "Payment customer does not match credit account"
            );
        }

        Payment cancelledPayment = payment.cancel();

        CreditEntry refundedCredit =
                credit.addCreditAmount(payment.price());

        return new PaymentOutcome(
                cancelledPayment,
                refundedCredit,
                Optional.empty()
        );
    }
}
