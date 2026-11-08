package kh.edu.istad.iam.restapi;

import jakarta.validation.Valid;
import kh.edu.istad.iam.application.command.AssignPermissionsToRoleCommand;
import kh.edu.istad.iam.application.command.CreateRoleCommand;
import kh.edu.istad.iam.application.command.UpdateRoleCommand;
import kh.edu.istad.iam.application.query.RoleFilterQuery;
import kh.edu.istad.iam.application.query.SortDirection;
import kh.edu.istad.iam.application.result.PageResult;
import kh.edu.istad.iam.application.result.PermissionAssignmentResult;
import kh.edu.istad.iam.application.result.RoleResult;
import kh.edu.istad.iam.application.result.RoleSummary;
import kh.edu.istad.iam.application.usecase.*;
import kh.edu.istad.iam.restapi.dto.RolePermissionRequests;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/roles")
public class RoleController {
    private final ObjectProvider<CreateRoleUseCase> create;
    private final ObjectProvider<ListRolesUseCase> list;
    private final ObjectProvider<GetRoleUseCase> get;
    private final ObjectProvider<UpdateRoleUseCase> update;
    private final ObjectProvider<DeleteRoleUseCase> delete;
    private final ObjectProvider<ActivateRoleUseCase> activate;
    private final ObjectProvider<DeactivateRoleUseCase> deactivate;
    private final ObjectProvider<AssignPermissionsToRoleUseCase> assignPermissions;
    private final ObjectProvider<RevokePermissionFromRoleUseCase> revokePermission;

    public RoleController(ObjectProvider<CreateRoleUseCase> create, ObjectProvider<ListRolesUseCase> list,
                          ObjectProvider<GetRoleUseCase> get, ObjectProvider<UpdateRoleUseCase> update,
                          ObjectProvider<DeleteRoleUseCase> delete, ObjectProvider<ActivateRoleUseCase> activate,
                          ObjectProvider<DeactivateRoleUseCase> deactivate,
                          ObjectProvider<AssignPermissionsToRoleUseCase> assignPermissions,
                          ObjectProvider<RevokePermissionFromRoleUseCase> revokePermission) {
        this.create = create; this.list = list; this.get = get; this.update = update; this.delete = delete;
        this.activate = activate; this.deactivate = deactivate; this.assignPermissions = assignPermissions;
        this.revokePermission = revokePermission;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('role:create')")
    public RoleResult create(@Valid @RequestBody RolePermissionRequests.RoleCreate body, Authentication auth) {
        RoleResult role = required(create, "CreateRoleUseCase").execute(
                new CreateRoleCommand(body.name(), body.description(), auth.getName()));
        if (!body.permissionUuids().isEmpty()) {
            if (body.permissionUuids().stream().distinct().count() != body.permissionUuids().size()) {
                throw new IllegalArgumentException("permissionUuids must not contain duplicates.");
            }
            required(assignPermissions, "AssignPermissionsToRoleUseCase").execute(
                    new AssignPermissionsToRoleCommand(role.uuid(), body.permissionUuids(), auth.getName(), null,
                            "Initial permissions assigned during role creation"));
        }
        return role;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('role:read')")
    public PageResult<RoleSummary> list(@RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "20") int size,
                                        @RequestParam(defaultValue = "name,asc") String sort,
                                        @RequestParam(required = false) String search,
                                        @RequestParam(required = false) Boolean active,
                                        @RequestParam(required = false) Boolean system) {
        String[] parts = sort.split(",", 2);
        SortDirection direction = parts.length == 1 ? SortDirection.ASC : SortDirection.valueOf(parts[1].toUpperCase());
        return required(list, "ListRolesUseCase").execute(new RoleFilterQuery(search, system, active, false,
                page, size, parts[0], direction));
    }

    @GetMapping("/{uuid}")
    @PreAuthorize("hasAuthority('role:read')")
    public RoleResult get(@PathVariable UUID uuid) { return required(get, "GetRoleUseCase").execute(uuid); }

    @PatchMapping("/{uuid}")
    @PreAuthorize("hasAuthority('role:update')")
    public RoleResult update(@PathVariable UUID uuid, @Valid @RequestBody RolePermissionRequests.RoleUpdate body,
                             Authentication auth) {
        return required(update, "UpdateRoleUseCase").execute(new UpdateRoleCommand(uuid, body.name(), body.description(), auth.getName()));
    }

    @DeleteMapping("/{uuid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('role:delete')")
    public void delete(@PathVariable UUID uuid, Authentication auth) {
        required(delete, "DeleteRoleUseCase").execute(uuid, Instant.now(), auth.getName());
    }

    @PostMapping("/{uuid}/activate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('role:update')")
    public void activate(@PathVariable UUID uuid, Authentication auth) {
        required(activate, "ActivateRoleUseCase").execute(uuid, Instant.now(), auth.getName());
    }

    @PostMapping("/{uuid}/deactivate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('role:update')")
    public void deactivate(@PathVariable UUID uuid, Authentication auth) {
        required(deactivate, "DeactivateRoleUseCase").execute(uuid, Instant.now(), auth.getName());
    }

    @PostMapping("/{uuid}/permissions")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('role:assign')")
    public List<PermissionAssignmentResult> assignPermissions(@PathVariable UUID uuid,
            @Valid @RequestBody RolePermissionRequests.PermissionAssignments body, Authentication auth) {
        return required(assignPermissions, "AssignPermissionsToRoleUseCase").execute(
                new AssignPermissionsToRoleCommand(uuid, body.permissionUuids(), auth.getName(), body.expiresAt(), body.reason()));
    }

    @DeleteMapping("/{uuid}/permissions/{permissionUuid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('role:assign')")
    public void revokePermission(@PathVariable UUID uuid, @PathVariable UUID permissionUuid,
                                 @RequestParam(required = false) String reason, Authentication auth) {
        required(revokePermission, "RevokePermissionFromRoleUseCase").execute(
                uuid, permissionUuid, reason, auth.getName(), Instant.now());
    }

    private static <T> T required(ObjectProvider<T> provider, String name) {
        T bean = provider.getIfAvailable();
        if (bean == null) throw new UseCaseUnavailableException(name);
        return bean;
    }
}
