package kh.edu.istad.iam.application.usecase;

import kh.edu.istad.iam.application.command.UpdateUserProfileCommand;
import kh.edu.istad.iam.application.result.UserResult;

public interface UpdateUserProfileUseCase {
    UserResult execute(UpdateUserProfileCommand command);
}
