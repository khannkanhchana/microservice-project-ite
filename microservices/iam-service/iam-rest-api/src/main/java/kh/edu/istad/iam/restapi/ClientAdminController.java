package kh.edu.istad.iam.restapi;

import kh.edu.istad.iam.application.admin.AdminModels.ClientCommand;
import kh.edu.istad.iam.application.admin.AdminModels.ClientDetails;
import kh.edu.istad.iam.application.admin.AdminModels.ClientWithSecret;
import kh.edu.istad.iam.application.admin.ClientAdminUseCase;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * OAuth2/OIDC client administration: register, view, update, delete, rotate secret.
 *
 * <p>Served at {@code /api/clients} and, for existing callers, {@code /api/admin/clients}. {@code {clientId}} accepts
 * either the client ID (e.g. {@code iam-web}) or the internal registration id.
 */
@RestController
@RequestMapping("/api")
public class ClientAdminController {
    private final ObjectProvider<ClientAdminUseCase> clients;

    public ClientAdminController(ObjectProvider<ClientAdminUseCase> clients) {
        this.clients = clients;
    }

    @GetMapping({"/clients", "/admin/clients"})
    @PreAuthorize("hasAuthority('client:read')")
    public List<ClientDetails> list(@RequestParam(name = "search", required = false) String search) {
        return service().list(search);
    }

    @GetMapping({"/clients/{clientId}", "/admin/clients/{clientId}"})
    @PreAuthorize("hasAuthority('client:read')")
    public ClientDetails get(@PathVariable("clientId") String clientId) {
        return service().get(clientId);
    }

    @PostMapping({"/clients", "/admin/clients"})
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('client:create')")
    public ClientWithSecret create(@RequestBody ClientCommand body, Authentication auth) {
        return service().create(body, auth.getName());
    }

    /** Replaces every editable setting. */
    @PutMapping({"/clients/{clientId}", "/admin/clients/{clientId}"})
    @PreAuthorize("hasAuthority('client:update')")
    public ClientWithSecret replace(@PathVariable("clientId") String clientId, @RequestBody ClientCommand body,
                                    Authentication auth) {
        return service().update(clientId, body, auth.getName());
    }

    /** Changes only the settings present in the body; omitted (null) fields keep their current value. */
    @PatchMapping({"/clients/{clientId}", "/admin/clients/{clientId}"})
    @PreAuthorize("hasAuthority('client:update')")
    public ClientWithSecret update(@PathVariable("clientId") String clientId, @RequestBody ClientPatch body,
                                   Authentication auth) {
        ClientDetails current = service().get(clientId);
        return service().update(current.id(), body.mergeInto(current), auth.getName());
    }

    @PostMapping({"/clients/{clientId}/secret", "/admin/clients/{clientId}/secret"})
    @PreAuthorize("hasAuthority('client:update')")
    public ClientWithSecret rotateSecret(@PathVariable("clientId") String clientId, Authentication auth) {
        return service().rotateSecret(clientId, auth.getName());
    }

    @DeleteMapping({"/clients/{clientId}", "/admin/clients/{clientId}"})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('client:delete')")
    public void delete(@PathVariable("clientId") String clientId, Authentication auth) {
        service().delete(clientId, auth.getName());
    }

    private ClientAdminUseCase service() {
        ClientAdminUseCase service = clients.getIfAvailable();
        if (service == null) throw new IllegalStateException("ClientAdminUseCase is not wired.");
        return service;
    }

    /** PATCH body: like {@link ClientCommand}, but every field is optional. The client ID cannot be changed. */
    public record ClientPatch(String clientName, List<String> authenticationMethods, List<String> grantTypes,
                              List<String> redirectUris, List<String> postLogoutRedirectUris, List<String> scopes,
                              Boolean requireProofKey, Boolean requireConsent, Integer accessTokenMinutes,
                              Integer refreshTokenDays, Boolean reuseRefreshTokens) {

        ClientCommand mergeInto(ClientDetails c) {
            return new ClientCommand(
                    c.clientId(),
                    clientName != null ? clientName : c.clientName(),
                    authenticationMethods != null ? authenticationMethods : c.authenticationMethods(),
                    grantTypes != null ? grantTypes : c.grantTypes(),
                    redirectUris != null ? redirectUris : c.redirectUris(),
                    postLogoutRedirectUris != null ? postLogoutRedirectUris : c.postLogoutRedirectUris(),
                    scopes != null ? scopes : c.scopes(),
                    requireProofKey != null ? requireProofKey : c.requireProofKey(),
                    requireConsent != null ? requireConsent : c.requireConsent(),
                    accessTokenMinutes != null ? accessTokenMinutes : (int) c.accessTokenMinutes(),
                    refreshTokenDays != null ? refreshTokenDays : (int) c.refreshTokenDays(),
                    reuseRefreshTokens != null ? reuseRefreshTokens : c.reuseRefreshTokens());
        }
    }
}
