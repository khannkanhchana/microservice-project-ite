package kh.edu.istad.iam.application.usecase;

import java.time.Instant;
import java.util.UUID;

public interface DeactivatePermissionUseCase {
    void execute(UUID permissionUuid, Instant at, String actor);
}
