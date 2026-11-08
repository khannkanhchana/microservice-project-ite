package kh.edu.istad.iam.application.usecase;

import kh.edu.istad.iam.application.command.GrantPermissionsToUserCommand;
import kh.edu.istad.iam.application.result.PermissionGrantResult;

import java.util.List;

public interface GrantPermissionsToUserUseCase {
    List<PermissionGrantResult> execute(GrantPermissionsToUserCommand command);
}
