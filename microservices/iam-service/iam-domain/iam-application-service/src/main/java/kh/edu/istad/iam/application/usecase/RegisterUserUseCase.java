package kh.edu.istad.iam.application.usecase;

import kh.edu.istad.iam.application.command.RegisterUserCommand;
import kh.edu.istad.iam.application.result.UserResult;

public interface RegisterUserUseCase {
    UserResult execute(RegisterUserCommand command);
}
