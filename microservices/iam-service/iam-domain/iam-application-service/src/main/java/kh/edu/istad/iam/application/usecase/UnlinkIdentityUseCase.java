package kh.edu.istad.iam.application.usecase;

import java.time.Instant;
import java.util.UUID;

public interface UnlinkIdentityUseCase {
    void execute(UUID userUuid, UUID identityUuid, Instant at, String actor);
}
