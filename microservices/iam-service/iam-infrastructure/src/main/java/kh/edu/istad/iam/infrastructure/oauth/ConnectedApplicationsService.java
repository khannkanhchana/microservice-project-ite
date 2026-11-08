package kh.edu.istad.iam.infrastructure.oauth;

import kh.edu.istad.iam.application.port.out.ConnectedApplicationsRepository;
import kh.edu.istad.iam.application.result.ConnectedApplicationResult;
import kh.edu.istad.iam.application.usecase.ListConnectedApplicationsUseCase;
import kh.edu.istad.iam.application.usecase.RevokeConnectedApplicationUseCase;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ConnectedApplicationsService implements ListConnectedApplicationsUseCase, RevokeConnectedApplicationUseCase {
    private final ConnectedApplicationsRepository repository;

    public ConnectedApplicationsService(ConnectedApplicationsRepository repository) {
        this.repository = repository;
    }

    @Override public List<ConnectedApplicationResult> execute(UUID userUuid) {
        return repository.findConsentedApplications(userUuid);
    }

    @Override public void execute(UUID userUuid, String clientId) {
        repository.revokeConsent(userUuid, clientId);
    }
}
