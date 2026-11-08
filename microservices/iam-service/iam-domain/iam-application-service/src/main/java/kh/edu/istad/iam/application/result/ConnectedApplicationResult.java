package kh.edu.istad.iam.application.result;

import java.util.Set;

public record ConnectedApplicationResult(String clientId, String clientName, Set<String> scopes) {
    public ConnectedApplicationResult { scopes = Set.copyOf(scopes); }
}
