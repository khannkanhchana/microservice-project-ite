package kh.edu.istad.iam.application.usecase;

import kh.edu.istad.iam.application.command.AssignPermissionsToRoleCommand;
import kh.edu.istad.iam.application.result.PermissionAssignmentResult;

import java.util.List;

public interface AssignPermissionsToRoleUseCase {
    List<PermissionAssignmentResult> execute(AssignPermissionsToRoleCommand command);
}
