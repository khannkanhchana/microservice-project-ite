package kh.edu.istad.iam.application.usecase;

import java.time.Instant;
import java.util.UUID;

public interface ActivateRoleUseCase {
    void execute(UUID roleUuid, Instant at, String actor);
}
