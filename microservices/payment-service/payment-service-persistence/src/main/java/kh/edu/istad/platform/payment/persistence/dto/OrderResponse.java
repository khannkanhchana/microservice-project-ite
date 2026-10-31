package kh.edu.istad.platform.payment.persistence.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.UUID;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
public record OrderResponse(
        @JsonProperty("orderId") UUID id,
        BigDecimal totalAmount,
        UUID customerId,
        String status
) {
}
