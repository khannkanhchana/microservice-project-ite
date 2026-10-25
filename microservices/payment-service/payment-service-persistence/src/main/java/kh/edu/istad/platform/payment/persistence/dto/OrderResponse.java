package kh.edu.istad.platform.payment.persistence.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderResponse(
        @JsonProperty("orderId") UUID id,
        BigDecimal totalAmount
) {
}
