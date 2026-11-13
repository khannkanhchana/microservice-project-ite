package kh.edu.istad.iam.application.account;

import java.util.UUID;

/** One-time, short-lived permission for a browser to link a social provider to an account. */
public record LinkTicket(UUID userUuid, String provider, String returnUrl) { }
