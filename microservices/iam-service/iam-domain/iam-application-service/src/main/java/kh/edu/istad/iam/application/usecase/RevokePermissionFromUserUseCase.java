package kh.edu.istad.iam.application.usecase;

import java.time.Instant;
import java.util.UUID;

public interface RevokePermissionFromUserUseCase {
    void execute(UUID userUuid, UUID permissionUuid, String reason, String revokedBy, Instant revokedAt);
}
