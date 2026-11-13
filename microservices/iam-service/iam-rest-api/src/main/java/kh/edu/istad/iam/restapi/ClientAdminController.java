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

/** OAuth2/OIDC client administration: register, view, update, delete, rotate secret. */
@RestController
@RequestMapping("/api/admin/clients")
public class ClientAdminController {
    private final ObjectProvider<ClientAdminUseCase> clients;

    public ClientAdminController(ObjectProvider<ClientAdminUseCase> clients) {
        this.clients = clients;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('client:read')")
    public List<ClientDetails> list(@RequestParam(name = "search", required = false) String search) {
        return service().list(search);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('client:read')")
    public ClientDetails get(@PathVariable("id") String id) {
        return service().get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('client:create')")
    public ClientWithSecret create(@RequestBody ClientCommand body, Authentication auth) {
        return service().create(body, auth.getName());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('client:update')")
    public ClientWithSecret update(@PathVariable("id") String id, @RequestBody ClientCommand body, Authentication auth) {
        return service().update(id, body, auth.getName());
    }

    @PostMapping("/{id}/secret")
    @PreAuthorize("hasAuthority('client:update')")
    public ClientWithSecret rotateSecret(@PathVariable("id") String id, Authentication auth) {
        return service().rotateSecret(id, auth.getName());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('client:delete')")
    public void delete(@PathVariable("id") String id, Authentication auth) {
        service().delete(id, auth.getName());
    }

    private ClientAdminUseCase service() {
        ClientAdminUseCase service = clients.getIfAvailable();
        if (service == null) throw new IllegalStateException("ClientAdminUseCase is not wired.");
        return service;
    }
}
