package kh.edu.istad.iam.application.command;

import java.util.UUID;

public record ChangePasswordCommand(UUID userUuid, String currentPassword, String newPassword, String changedBy) { }
