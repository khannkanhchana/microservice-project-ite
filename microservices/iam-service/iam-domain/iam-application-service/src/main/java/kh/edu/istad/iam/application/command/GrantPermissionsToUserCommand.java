package kh.edu.istad.iam.application.command;

import kh.edu.istad.iam.domain.AssignmentEffect;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record GrantPermissionsToUserCommand(UUID userUuid, List<UUID> permissionUuids,
                                            AssignmentEffect effect, String assignedBy,
                                            Instant expiresAt, String reason) {
    public GrantPermissionsToUserCommand {
        permissionUuids = List.copyOf(permissionUuids);
    }
}
