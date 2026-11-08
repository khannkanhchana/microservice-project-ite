package kh.edu.istad.iam.application.usecase;

import java.time.Instant;
import java.util.UUID;

public interface RevokePermissionFromRoleUseCase {
    void execute(UUID roleUuid, UUID permissionUuid, String reason, String revokedBy, Instant revokedAt);
}
