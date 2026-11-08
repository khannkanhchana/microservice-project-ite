package kh.edu.istad.iam.application.usecase;

import kh.edu.istad.iam.application.command.AssignRolesToUserCommand;
import kh.edu.istad.iam.application.result.RoleAssignmentResult;

import java.util.List;

public interface AssignRolesToUserUseCase {
    List<RoleAssignmentResult> execute(AssignRolesToUserCommand command);
}
