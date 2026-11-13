package kh.edu.istad.platform.customer.message;


import java.time.ZonedDateTime;
import java.util.UUID;

public record CustomerInitiatedMessage(
        UUID customerId,
        String username,
        String familyName,
        String givenName,
        String email,
        String phoneNumber,
        String status,
        ZonedDateTime initiatedAt
) {
}