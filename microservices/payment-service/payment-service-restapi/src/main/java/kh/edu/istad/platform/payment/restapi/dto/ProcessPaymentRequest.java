package kh.edu.istad.platform.payment.restapi.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record ProcessPaymentRequest(
        @NotNull
        UUID orderId,

        @NotNull
        UUID customerId,

        @NotNull
        @DecimalMin("0.01")
        @jakarta.validation.constraints.Digits(integer=17, fraction=2)
        BigDecimal amount
) {
}
