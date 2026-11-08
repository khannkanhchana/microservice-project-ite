package kh.edu.istad.iam.application.usecase;

import kh.edu.istad.iam.application.result.IdentityResult;

import java.util.List;
import java.util.UUID;

public interface ListUserIdentitiesUseCase {
    List<IdentityResult> execute(UUID userUuid);
}
