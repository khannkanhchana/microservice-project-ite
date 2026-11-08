package kh.edu.istad.iam.application.result;

import java.time.Instant;
import java.util.UUID;

public record PermissionResult(UUID uuid, String name, String resource, String action,
                              String description, boolean system, boolean active, boolean deleted,
                              Instant createdAt, String createdBy, Instant updatedAt, String updatedBy,
                              Instant deletedAt, String deletedBy, long version) { }
