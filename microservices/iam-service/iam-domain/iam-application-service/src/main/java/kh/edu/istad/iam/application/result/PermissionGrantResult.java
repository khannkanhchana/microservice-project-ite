package kh.edu.istad.iam.application.result;

import kh.edu.istad.iam.domain.AssignmentEffect;

import java.time.Instant;
import java.util.UUID;

public record PermissionGrantResult(Long assignmentId, UUID permissionUuid, String permissionName,
                                    AssignmentEffect effect, Instant assignedAt, String assignedBy,
                                    Instant expiresAt, Instant revokedAt, String revokedBy, String reason) { }
