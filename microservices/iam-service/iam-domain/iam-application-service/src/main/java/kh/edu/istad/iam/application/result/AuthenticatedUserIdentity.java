package kh.edu.istad.iam.application.result;

import java.util.UUID;

/** Stable application identity exposed by security principals to adapters. */
public interface AuthenticatedUserIdentity {
    UUID getUserUuid();
}
