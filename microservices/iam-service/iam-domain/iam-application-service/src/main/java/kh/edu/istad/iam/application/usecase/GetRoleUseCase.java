package kh.edu.istad.iam.application.usecase;

import kh.edu.istad.iam.application.result.RoleResult;

import java.util.UUID;

public interface GetRoleUseCase {
    RoleResult execute(UUID roleUuid);
}
