package kh.edu.istad.iam.application.account;

import java.time.Instant;
import java.util.UUID;

/**
 * An account deactivation request. {@code status}: PENDING (takes effect at {@code scheduledFor} unless
 * cancelled), CANCELLED, or COMPLETED (account disabled at {@code processedAt}).
 */
public record DeactivationRequestResult(UUID id, String status, String reason, Instant requestedAt,
                                        Instant scheduledFor, Instant processedAt, Instant cancelledAt) {
    public boolean pending() {
        return "PENDING".equals(status);
    }
}
