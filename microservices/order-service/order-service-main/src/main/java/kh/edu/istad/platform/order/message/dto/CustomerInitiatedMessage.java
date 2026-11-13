package kh.edu.istad.platform.order.message.dto;

import java.time.Instant;
import java.util.UUID;

public record CustomerInitiatedMessage(
        UUID customerId,
        String username,
        String familyName,
        String givenName,
        String email,
        String phoneNumber,
        String status,
        Instant initiatedAt
) {
}