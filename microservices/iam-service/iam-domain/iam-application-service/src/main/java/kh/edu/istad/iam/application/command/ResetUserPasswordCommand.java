package kh.edu.istad.iam.application.command;

import java.util.UUID;

public record ResetUserPasswordCommand(UUID userUuid, String newPassword, String resetBy) { }
