package kh.edu.istad.iam.application.usecase;

import java.time.Instant;
import java.util.UUID;

public interface DeactivateRoleUseCase {
    void execute(UUID roleUuid, Instant at, String actor);
}
