package kh.edu.istad.iam.application.usecase;

import java.time.Instant;
import java.util.UUID;

public interface DisableUserUseCase {
    void execute(UUID userUuid, Instant at, String actor);
}
