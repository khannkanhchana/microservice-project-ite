package kh.edu.istad.iam.application.account;

import java.time.Instant;
import java.util.UUID;

public record LoginAttemptResult(UUID id, boolean success, String method, String failureReason,
                                 String ipAddress, String location, String browser, String os,
                                 String deviceType, Instant attemptedAt) { }
