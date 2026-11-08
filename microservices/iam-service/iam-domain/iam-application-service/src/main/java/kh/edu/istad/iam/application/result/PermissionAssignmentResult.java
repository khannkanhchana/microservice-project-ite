package kh.edu.istad.iam.application.result;

import java.time.Instant;
import java.util.UUID;

public record PermissionAssignmentResult(Long assignmentId, UUID permissionUuid, String permissionName,
                                         Instant assignedAt, String assignedBy, Instant expiresAt,
                                         Instant revokedAt, String revokedBy, String reason) { }
