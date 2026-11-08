package kh.edu.istad.iam.application.command;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AssignRolesToUserCommand(UUID userUuid, List<UUID> roleUuids, String assignedBy,
                                      Instant expiresAt, String reason) {
    public AssignRolesToUserCommand {
        roleUuids = List.copyOf(roleUuids);
    }
}
