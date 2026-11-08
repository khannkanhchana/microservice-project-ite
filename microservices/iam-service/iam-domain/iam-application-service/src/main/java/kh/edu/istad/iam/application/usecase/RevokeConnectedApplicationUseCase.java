package kh.edu.istad.iam.application.usecase;

import java.util.UUID;

public interface RevokeConnectedApplicationUseCase {
    void execute(UUID userUuid, String clientId);
}
