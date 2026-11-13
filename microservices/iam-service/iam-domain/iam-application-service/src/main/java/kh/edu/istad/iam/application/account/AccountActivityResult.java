package kh.edu.istad.iam.application.account;

import java.time.Instant;
import java.util.UUID;

public record AccountActivityResult(UUID id, String type, String description, String ipAddress,
                                    String browser, String os, String deviceType, Instant occurredAt) { }
