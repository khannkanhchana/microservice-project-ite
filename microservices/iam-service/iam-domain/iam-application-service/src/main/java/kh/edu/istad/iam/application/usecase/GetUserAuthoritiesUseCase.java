package kh.edu.istad.iam.application.usecase;

import kh.edu.istad.iam.application.result.UserAuthoritiesResult;

import java.util.UUID;

public interface GetUserAuthoritiesUseCase {
    UserAuthoritiesResult execute(UUID userUuid);
}
