package kh.edu.istad.iam.application.usecase;

import kh.edu.istad.iam.application.command.CreatePermissionCommand;
import kh.edu.istad.iam.application.result.PermissionResult;

public interface CreatePermissionUseCase {
    PermissionResult execute(CreatePermissionCommand command);
}
