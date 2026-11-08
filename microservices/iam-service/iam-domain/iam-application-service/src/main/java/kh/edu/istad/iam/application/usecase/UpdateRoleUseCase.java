package kh.edu.istad.iam.application.usecase;

import kh.edu.istad.iam.application.command.UpdateRoleCommand;
import kh.edu.istad.iam.application.result.RoleResult;

public interface UpdateRoleUseCase {
    RoleResult execute(UpdateRoleCommand command);
}
