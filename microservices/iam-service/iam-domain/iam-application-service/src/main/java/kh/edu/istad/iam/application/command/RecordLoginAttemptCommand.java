package kh.edu.istad.iam.application.command;

import java.time.Instant;

public record RecordLoginAttemptCommand(String usernameOrEmail, boolean success, Instant attemptedAt, String actor) { }
