package kh.edu.istad.iam.application.command;

public record CreateRoleCommand(String name, String description, String createdBy) { }
