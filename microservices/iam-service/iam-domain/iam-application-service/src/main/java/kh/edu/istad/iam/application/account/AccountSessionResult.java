package kh.edu.istad.iam.application.account;

import java.time.Instant;
import java.util.UUID;

/** A signed-in device: one browser login to the IAM server and every token issued from it. */
public record AccountSessionResult(UUID id, String loginMethod, String browser, String os, String deviceType,
                                   String ipAddress, String location, Instant createdAt, Instant lastActivityAt,
                                   boolean current) { }
