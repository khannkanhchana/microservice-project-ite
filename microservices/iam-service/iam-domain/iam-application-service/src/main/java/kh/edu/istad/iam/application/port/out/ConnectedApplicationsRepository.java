package kh.edu.istad.iam.application.port.out;

import kh.edu.istad.iam.application.result.ConnectedApplicationResult;

import java.util.List;
import java.util.UUID;

public interface ConnectedApplicationsRepository {
    List<ConnectedApplicationResult> findConsentedApplications(UUID userUuid);
    void revokeConsent(UUID userUuid, String clientId);
}
