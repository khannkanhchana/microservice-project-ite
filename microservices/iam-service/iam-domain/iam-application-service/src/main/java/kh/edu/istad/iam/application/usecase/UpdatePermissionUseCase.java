package kh.edu.istad.iam.application.usecase;

import kh.edu.istad.iam.application.command.UpdatePermissionCommand;
import kh.edu.istad.iam.application.result.PermissionResult;

public interface UpdatePermissionUseCase {
    PermissionResult execute(UpdatePermissionCommand command);
}
