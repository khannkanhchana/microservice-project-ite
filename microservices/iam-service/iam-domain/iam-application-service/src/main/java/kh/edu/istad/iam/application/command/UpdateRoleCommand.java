package kh.edu.istad.iam.application.command;

import java.util.UUID;

public record UpdateRoleCommand(UUID roleUuid, String name, String description, String updatedBy) { }
