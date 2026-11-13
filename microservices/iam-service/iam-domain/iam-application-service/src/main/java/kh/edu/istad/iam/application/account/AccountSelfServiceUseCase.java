package kh.edu.istad.iam.application.account;

import kh.edu.istad.iam.application.result.PageResult;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Self-service account security for the signed-in user: device sessions, login history,
 * security activity, contact details, profile picture, deactivation and password recovery.
 */
public interface AccountSelfServiceUseCase {

    boolean hasPassword(UUID userUuid);

    /** Changes username, email and phone number; a blank phone clears it, null leaves a field unchanged. */
    List<String> updateContact(UUID userUuid, String username, String email, String phoneNumber, String actor);

    List<AccountSessionResult> sessions(UUID userUuid, UUID currentSessionId);

    void revokeSession(UUID userUuid, UUID sessionId, String actor);

    /** Revokes every session except {@code keepSessionId}; pass null to sign out everywhere. */
    int revokeOtherSessions(UUID userUuid, UUID keepSessionId, String actor);

    PageResult<LoginAttemptResult> loginHistory(UUID userUuid, Boolean success, int page, int size);

    PageResult<AccountActivityResult> activity(UUID userUuid, int page, int size);

    void recordActivity(UUID userUuid, AccountActivityType type, String description, String actor);

    void deactivate(UUID userUuid, String password, String reason, String actor);

    String storePicture(UUID userUuid, String contentType, byte[] data, String publicBaseUrl, String actor);

    void removePicture(UUID userUuid, String actor);

    Optional<StoredPicture> picture(UUID userUuid);

    String createLinkTicket(UUID userUuid, String provider, String returnUrl);

    Optional<LinkTicket> consumeLinkTicket(String ticket);

    /** Always succeeds so callers cannot probe which emails are registered. */
    void requestPasswordReset(String email, String resetPageUrl);

    void resetPassword(String token, String newPassword);
}
