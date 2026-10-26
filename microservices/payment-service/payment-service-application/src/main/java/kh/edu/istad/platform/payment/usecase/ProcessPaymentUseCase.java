package kh.edu.istad.platform.payment.usecase;


import kh.edu.istad.platform.payment.domain.entity.CreditEntry;
import kh.edu.istad.platform.payment.domain.entity.Payment;

import kh.edu.istad.platform.payment.domain.service.PaymentDomainService;
import kh.edu.istad.platform.payment.domain.service.PaymentOutcome;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.Objects;

public class ProcessPaymentUseCase {

    private final PaymentDomainService service;
    private final Clock clock;

    public ProcessPaymentUseCase(
            PaymentDomainService service,
            Clock clock
    ) {
        this.service = Objects.requireNonNull(service);
        this.clock = Objects.requireNonNull(clock);
    }

    public PaymentOutcome execute(
            Payment payment,
            CreditEntry credit
    ) {
        return service.process(
                payment,
                credit,
                ZonedDateTime.now(clock)
        );
    }

    public PaymentOutcome cancel(
            Payment payment,
            CreditEntry credit
    ) {
        return service.cancel(
                payment,
                credit,
                ZonedDateTime.now(clock)
        );
    }
}