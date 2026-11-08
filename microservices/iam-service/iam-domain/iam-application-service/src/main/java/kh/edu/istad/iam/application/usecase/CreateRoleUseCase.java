package kh.edu.istad.iam.application.usecase;

import kh.edu.istad.iam.application.command.CreateRoleCommand;
import kh.edu.istad.iam.application.result.RoleResult;

public interface CreateRoleUseCase {
    RoleResult execute(CreateRoleCommand command);
}
