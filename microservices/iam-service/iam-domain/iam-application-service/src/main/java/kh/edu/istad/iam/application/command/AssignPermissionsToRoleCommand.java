package kh.edu.istad.iam.application.command;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AssignPermissionsToRoleCommand(UUID roleUuid, List<UUID> permissionUuids,
                                             String assignedBy, Instant expiresAt, String reason) {
    public AssignPermissionsToRoleCommand {
        permissionUuids = List.copyOf(permissionUuids);
    }
}
