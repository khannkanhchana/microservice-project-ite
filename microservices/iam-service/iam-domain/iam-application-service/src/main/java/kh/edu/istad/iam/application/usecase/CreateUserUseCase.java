package kh.edu.istad.iam.application.usecase;

import kh.edu.istad.iam.application.command.CreateUserCommand;
import kh.edu.istad.iam.application.result.UserResult;

public interface CreateUserUseCase {
    UserResult execute(CreateUserCommand command);
}
