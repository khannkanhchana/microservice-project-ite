package kh.edu.istad.iam.application.result;

import java.time.Instant;
import java.util.UUID;

public record UserSummary(UUID uuid, String username, String name, String email,
                          boolean emailVerified, boolean enabled, Instant createdAt) { }
