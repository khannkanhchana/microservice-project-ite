package kh.edu.istad.iam.infrastructure.oauth;

import kh.edu.istad.iam.application.port.out.ConnectedApplicationsRepository;
import kh.edu.istad.iam.application.result.ConnectedApplicationResult;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class JdbcConnectedApplicationsRepository implements ConnectedApplicationsRepository {
    private final JdbcTemplate jdbcTemplate;
    private final RegisteredClientRepository clients;
    private final OAuth2AuthorizationConsentService consents;

    public JdbcConnectedApplicationsRepository(JdbcTemplate jdbcTemplate,
                                               RegisteredClientRepository clients,
                                               OAuth2AuthorizationConsentService consents) {
        this.jdbcTemplate = jdbcTemplate;
        this.clients = clients;
        this.consents = consents;
    }

    @Override
    public List<ConnectedApplicationResult> findConsentedApplications(UUID userUuid) {
        List<String> clientIds = jdbcTemplate.queryForList(
                "select registered_client_id from oauth2_authorization_consent where principal_name = ?",
                String.class, userUuid.toString());
        return clientIds.stream().map(clients::findById).filter(java.util.Objects::nonNull)
                .map(registeredClient -> new ConnectedApplicationResult(registeredClient.getClientId(),
                        registeredClient.getClientName(), scopesFor(registeredClient, userUuid))).toList();
    }

    @Override
    public void revokeConsent(UUID userUuid, String clientId) {
        RegisteredClient client = clients.findByClientId(clientId);
        if (client == null) return;
        var consent = consents.findById(client.getId(), userUuid.toString());
        if (consent != null) consents.remove(consent);
    }

    private Set<String> scopesFor(RegisteredClient client, UUID userUuid) {
        var consent = consents.findById(client.getId(), userUuid.toString());
        if (consent == null) return Set.of();
        return consent.getAuthorities().stream().map(authority -> authority.getAuthority())
                .filter(value -> value.startsWith("SCOPE_"))
                .map(value -> value.substring("SCOPE_".length())).collect(Collectors.toUnmodifiableSet());
    }
}
