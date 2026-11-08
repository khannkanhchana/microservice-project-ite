package kh.edu.istad.iam.application.usecase;

import kh.edu.istad.iam.application.command.ChangePasswordCommand;

public interface ChangePasswordUseCase {
    void execute(ChangePasswordCommand command);
}
