package kh.edu.istad.platform.order.application.dto;


import java.util.UUID;

public record CustomerSyncCommand(
        UUID customerId,
        String username,
        String familyName,
        String givenName
) {
}
