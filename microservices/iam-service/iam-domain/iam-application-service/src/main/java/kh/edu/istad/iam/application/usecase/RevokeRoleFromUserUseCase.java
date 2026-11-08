package kh.edu.istad.iam.application.usecase;

import java.time.Instant;
import java.util.UUID;

public interface RevokeRoleFromUserUseCase {
    void execute(UUID userUuid, UUID roleUuid, String reason, String revokedBy, Instant revokedAt);
}
