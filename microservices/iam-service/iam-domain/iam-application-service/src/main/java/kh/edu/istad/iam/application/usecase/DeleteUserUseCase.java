package kh.edu.istad.iam.application.usecase;

import java.time.Instant;
import java.util.UUID;

public interface DeleteUserUseCase {
    void execute(UUID userUuid, Instant at, String actor);
}
