package kh.edu.istad.iam.application.usecase;

import kh.edu.istad.iam.application.result.PermissionResult;

import java.util.UUID;

public interface GetPermissionUseCase {
    PermissionResult execute(UUID permissionUuid);
}
