package kh.edu.istad.iam.application.usecase;

import kh.edu.istad.iam.application.result.ConnectedApplicationResult;

import java.util.List;
import java.util.UUID;

public interface ListConnectedApplicationsUseCase {
    List<ConnectedApplicationResult> execute(UUID userUuid);
}
