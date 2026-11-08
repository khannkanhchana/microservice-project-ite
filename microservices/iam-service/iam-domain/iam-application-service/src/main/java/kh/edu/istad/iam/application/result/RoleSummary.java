package kh.edu.istad.iam.application.result;

import java.time.Instant;
import java.util.UUID;

public record RoleSummary(UUID uuid, String name, String description, boolean system,
                         boolean active, Instant createdAt) { }
