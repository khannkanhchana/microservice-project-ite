package kh.edu.istad.platform.order.application.dto;

import java.util.UUID;

public record CustomerResponse(
        UUID customerId,
        String username,
        String familyName,
        String givenName,
        String email,
        String phoneNumber
) {
}