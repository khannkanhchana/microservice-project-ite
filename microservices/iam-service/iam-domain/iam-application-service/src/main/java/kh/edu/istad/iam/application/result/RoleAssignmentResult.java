package kh.edu.istad.iam.application.result;

import java.time.Instant;
import java.util.UUID;

public record RoleAssignmentResult(Long assignmentId, UUID roleUuid, String roleName,
                                   Instant assignedAt, String assignedBy, Instant expiresAt,
                                   Instant revokedAt, String revokedBy, String reason) { }
