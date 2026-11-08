package kh.edu.istad.iam.application.usecase;

import kh.edu.istad.iam.application.command.FederatedIdentityCommand;
import kh.edu.istad.iam.application.result.IdentityResult;

import java.util.UUID;

public interface LinkIdentityUseCase {
    IdentityResult execute(UUID userUuid, FederatedIdentityCommand command);
}
