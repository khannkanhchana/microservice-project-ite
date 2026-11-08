package kh.edu.istad.iam.application.usecase;

import kh.edu.istad.iam.application.command.RecordLoginAttemptCommand;

public interface RecordLoginAttemptUseCase {
    void execute(RecordLoginAttemptCommand command);
}
