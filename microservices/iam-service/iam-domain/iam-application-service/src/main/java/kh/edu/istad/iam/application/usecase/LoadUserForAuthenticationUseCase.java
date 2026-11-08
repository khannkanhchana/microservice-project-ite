package kh.edu.istad.iam.application.usecase;

import kh.edu.istad.iam.application.result.AuthUserResult;

public interface LoadUserForAuthenticationUseCase {
    AuthUserResult execute(String usernameOrEmail);
}
