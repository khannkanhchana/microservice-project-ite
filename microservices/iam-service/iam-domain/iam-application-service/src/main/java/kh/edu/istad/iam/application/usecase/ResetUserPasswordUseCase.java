package kh.edu.istad.iam.application.usecase;

import kh.edu.istad.iam.application.command.ResetUserPasswordCommand;

public interface ResetUserPasswordUseCase {
    void execute(ResetUserPasswordCommand command);
}
