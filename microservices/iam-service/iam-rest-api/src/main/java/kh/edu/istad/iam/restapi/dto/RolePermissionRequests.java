package kh.edu.istad.iam.restapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class RolePermissionRequests {
    private RolePermissionRequests() { }

    public record RoleCreate(@NotBlank @Size(max = 50) String name,
                             @Size(max = 255) String description,
                             List<java.util.UUID> permissionUuids) {
        public RoleCreate {
            permissionUuids = permissionUuids == null ? List.of() : List.copyOf(permissionUuids);
        }
    }

    public record RoleUpdate(@NotBlank @Size(max = 50) String name,
                             @Size(max = 255) String description) { }

    public record PermissionCreate(@NotBlank @Size(max = 50) String resource,
                                   @NotBlank @Size(max = 50) String action,
                                   @Size(max = 255) String description) { }

    public record PermissionUpdate(@NotBlank @Size(max = 50) String resource,
                                   @NotBlank @Size(max = 50) String action,
                                   @Size(max = 255) String description) { }

    public record PermissionAssignments(@NotEmpty List<UUID> permissionUuids,
                                        Instant expiresAt, @Size(max = 255) String reason) { }
}
