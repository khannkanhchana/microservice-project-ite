package kh.edu.istad.iam.application.usecase;

import kh.edu.istad.iam.application.command.FederatedIdentityCommand;
import kh.edu.istad.iam.application.result.AuthUserResult;

public interface FindOrProvisionFederatedUserUseCase {
    AuthUserResult execute(FederatedIdentityCommand command);
}
