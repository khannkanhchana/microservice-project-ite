package kh.edu.istad.iam.restapi;

import jakarta.validation.Valid;
import kh.edu.istad.iam.application.command.CreatePermissionCommand;
import kh.edu.istad.iam.application.command.UpdatePermissionCommand;
import kh.edu.istad.iam.application.query.PermissionFilterQuery;
import kh.edu.istad.iam.application.query.SortDirection;
import kh.edu.istad.iam.application.result.PageResult;
import kh.edu.istad.iam.application.result.PermissionResult;
import kh.edu.istad.iam.application.usecase.*;
import kh.edu.istad.iam.restapi.dto.RolePermissionRequests;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/permissions")
public class PermissionController {
    private final ObjectProvider<CreatePermissionUseCase> create;
    private final ObjectProvider<ListPermissionsUseCase> list;
    private final ObjectProvider<GetPermissionUseCase> get;
    private final ObjectProvider<UpdatePermissionUseCase> update;
    private final ObjectProvider<DeletePermissionUseCase> delete;
    private final ObjectProvider<ActivatePermissionUseCase> activate;
    private final ObjectProvider<DeactivatePermissionUseCase> deactivate;

    public PermissionController(ObjectProvider<CreatePermissionUseCase> create,
                                ObjectProvider<ListPermissionsUseCase> list,
                                ObjectProvider<GetPermissionUseCase> get,
                                ObjectProvider<UpdatePermissionUseCase> update,
                                ObjectProvider<DeletePermissionUseCase> delete,
                                ObjectProvider<ActivatePermissionUseCase> activate,
                                ObjectProvider<DeactivatePermissionUseCase> deactivate) {
        this.create = create; this.list = list; this.get = get; this.update = update;
        this.delete = delete; this.activate = activate; this.deactivate = deactivate;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('permission:create')")
    public PermissionResult create(@Valid @RequestBody RolePermissionRequests.PermissionCreate body,
                                   Authentication auth) {
        return required(create, "CreatePermissionUseCase").execute(
                new CreatePermissionCommand(body.resource(), body.action(), body.description(), auth.getName()));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('permission:read')")
    public PageResult<PermissionResult> list(@RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size,
                                             @RequestParam(defaultValue = "name,asc") String sort,
                                             @RequestParam(required = false) String search,
                                             @RequestParam(required = false) String resource,
                                             @RequestParam(required = false) String action,
                                             @RequestParam(required = false) Boolean active,
                                             @RequestParam(required = false) Boolean system) {
        String[] parts = sort.split(",", 2);
        SortDirection direction = parts.length == 1 ? SortDirection.ASC : SortDirection.valueOf(parts[1].toUpperCase());
        return required(list, "ListPermissionsUseCase").execute(new PermissionFilterQuery(
                search, resource, action, system, active, false, page, size, parts[0], direction));
    }

    @GetMapping("/{uuid}")
    @PreAuthorize("hasAuthority('permission:read')")
    public PermissionResult get(@PathVariable UUID uuid) { return required(get, "GetPermissionUseCase").execute(uuid); }

    @PatchMapping("/{uuid}")
    @PreAuthorize("hasAuthority('permission:update')")
    public PermissionResult update(@PathVariable UUID uuid,
            @Valid @RequestBody RolePermissionRequests.PermissionUpdate body, Authentication auth) {
        return required(update, "UpdatePermissionUseCase").execute(new UpdatePermissionCommand(
                uuid, body.resource(), body.action(), body.description(), auth.getName()));
    }

    @DeleteMapping("/{uuid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('permission:delete')")
    public void delete(@PathVariable UUID uuid, Authentication auth) {
        required(delete, "DeletePermissionUseCase").execute(uuid, Instant.now(), auth.getName());
    }

    @PostMapping("/{uuid}/activate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('permission:update')")
    public void activate(@PathVariable UUID uuid, Authentication auth) {
        required(activate, "ActivatePermissionUseCase").execute(uuid, Instant.now(), auth.getName());
    }

    @PostMapping("/{uuid}/deactivate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('permission:update')")
    public void deactivate(@PathVariable UUID uuid, Authentication auth) {
        required(deactivate, "DeactivatePermissionUseCase").execute(uuid, Instant.now(), auth.getName());
    }

    private static <T> T required(ObjectProvider<T> provider, String name) {
        T bean = provider.getIfAvailable();
        if (bean == null) throw new UseCaseUnavailableException(name);
        return bean;
    }
}
