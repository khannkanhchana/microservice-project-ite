package kh.edu.istad.platform.payment.domain.service;

import kh.edu.istad.platform.payment.domain.entity.CreditEntry;
import kh.edu.istad.platform.payment.domain.entity.CreditHistory;
import kh.edu.istad.platform.payment.domain.entity.Payment;
import kh.edu.istad.platform.payment.domain.event.PaymentEvent;

import java.util.Optional;

public record PaymentOutcome(Payment payment, CreditEntry creditEntry, Optional<CreditHistory> history, PaymentEvent event) {}
