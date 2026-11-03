package kh.edu.istad.platform.order.application.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record QueryCustomerResult(
//        UUID orderId,
//        UUID trackingId,
//        OrderStatus status,
//        BigDecimal totalAmount,
        String familyName,
        String givenName


) {
}
