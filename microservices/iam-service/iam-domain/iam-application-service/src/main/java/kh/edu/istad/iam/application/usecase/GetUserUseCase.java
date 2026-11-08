package kh.edu.istad.iam.application.usecase;

import kh.edu.istad.iam.application.result.UserResult;

import java.util.UUID;

public interface GetUserUseCase {
    UserResult execute(UUID userUuid);
}
