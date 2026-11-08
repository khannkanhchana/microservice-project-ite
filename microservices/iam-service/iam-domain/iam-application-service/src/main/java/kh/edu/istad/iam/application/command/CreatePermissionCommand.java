package kh.edu.istad.iam.application.command;

public record CreatePermissionCommand(String resource, String action, String description, String createdBy) { }
