package kh.edu.istad.platform.payment.domain.event;

import kh.edu.istad.platform.payment.domain.entity.Payment;

import java.time.ZonedDateTime;
import java.util.List;

public sealed interface PaymentEvent permits PaymentCompletedEvent, PaymentCancelledEvent, PaymentFailedEvent {
    Payment payment();
    ZonedDateTime createdAt();
    List<String> failureMessages();
}
