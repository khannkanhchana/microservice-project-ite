package kh.edu.istad.iam.application.service;

import kh.edu.istad.iam.application.port.out.PermissionRepository;
import kh.edu.istad.iam.application.port.out.RolePermissionAssignmentRepository;
import kh.edu.istad.iam.application.port.out.RoleRepository;
import kh.edu.istad.iam.application.port.out.UserPermissionAssignmentRepository;
import kh.edu.istad.iam.application.port.out.UserRepository;
import kh.edu.istad.iam.application.port.out.UserRoleAssignmentRepository;
import kh.edu.istad.iam.application.result.UserAuthoritiesResult;
import kh.edu.istad.iam.application.usecase.GetUserAuthoritiesUseCase;
import kh.edu.istad.iam.domain.IamAuthorizationPolicy;
import kh.edu.istad.iam.domain.Permission;
import kh.edu.istad.iam.domain.Role;
import kh.edu.istad.iam.domain.RolePermissionAssignment;
import kh.edu.istad.iam.domain.User;
import kh.edu.istad.iam.domain.UserPermissionAssignment;
import kh.edu.istad.iam.domain.UserRoleAssignment;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Resolves the user's role and permission authorities using IAM policy and repository ports. */
public final class GetUserAuthoritiesService implements GetUserAuthoritiesUseCase {
    private final UserRepository users;
    private final UserRoleAssignmentRepository userRoles;
    private final UserPermissionAssignmentRepository userPermissions;
    private final RolePermissionAssignmentRepository rolePermissions;
    private final RoleRepository roles;
    private final PermissionRepository permissions;

    public GetUserAuthoritiesService(UserRepository users, UserRoleAssignmentRepository userRoles,
                                     UserPermissionAssignmentRepository userPermissions,
                                     RolePermissionAssignmentRepository rolePermissions,
                                     RoleRepository roles, PermissionRepository permissions) {
        this.users = users;
        this.userRoles = userRoles;
        this.userPermissions = userPermissions;
        this.rolePermissions = rolePermissions;
        this.roles = roles;
        this.permissions = permissions;
    }

    @Override
    public UserAuthoritiesResult execute(UUID userUuid) {
        User user = users.findByUuid(userUuid)
                .orElseThrow(() -> new IllegalArgumentException("User was not found."));
        Instant at = Instant.now();
        List<UserRoleAssignment> userRoleAssignments = userRoles.findByUserUuid(userUuid);
        List<UserPermissionAssignment> directAssignments = userPermissions.findByUserUuid(userUuid);
        List<Role> roleModels = roles.findByIds(userRoleAssignments.stream()
                .map(UserRoleAssignment::getRoleId).distinct().toList());
        List<RolePermissionAssignment> roleAssignments = rolePermissions.findByRoleUuids(roleModels.stream()
                .map(Role::uuid).toList());

        List<Long> permissionIds = java.util.stream.Stream.concat(
                        directAssignments.stream().map(UserPermissionAssignment::getPermissionId),
                        roleAssignments.stream().map(RolePermissionAssignment::getPermissionId))
                .distinct().toList();
        List<Permission> permissionModels = permissions.findByIds(permissionIds);
        Map<Long, Role> rolesById = index(roleModels, Role::id);
        Map<Long, Permission> permissionsById = index(permissionModels, Permission::id);

        var effectiveAuthorities = IamAuthorizationPolicy.resolveAuthorities(user, roleModels, permissionModels,
                userRoleAssignments, roleAssignments, directAssignments, at);
        List<UserAuthoritiesResult.RoleAuthority> activeRoles = userRoleAssignments.stream()
                .filter(assignment -> assignment.isActiveAt(at))
                .map(assignment -> rolesById.get(assignment.getRoleId()))
                .filter(role -> role != null && role.active() && !role.deleted())
                .distinct()
                .map(role -> new UserAuthoritiesResult.RoleAuthority(role.uuid(), role.name(),
                        userRoleAssignments.stream().filter(a -> a.getRoleId().equals(role.id()) && a.isActiveAt(at))
                                .map(a -> a.getExpiresAt()).filter(Objects::nonNull).min(Instant::compareTo).orElse(null)))
                .toList();
        List<UserAuthoritiesResult.DirectPermission> activeDirectPermissions = directAssignments.stream()
                .filter(assignment -> assignment.isActiveAt(at))
                .flatMap(assignment -> Optional.ofNullable(permissionsById.get(assignment.getPermissionId()))
                        .filter(permission -> permission.active() && !permission.deleted())
                        .map(permission -> new UserAuthoritiesResult.DirectPermission(permission.uuid(),
                                permission.name(), assignment.getEffect(), assignment.getExpiresAt())).stream())
                .toList();
        return new UserAuthoritiesResult(userUuid, activeRoles, activeDirectPermissions,
                effectiveAuthorities.stream().filter(authority -> !authority.startsWith("ROLE_"))
                        .collect(Collectors.toSet()), effectiveAuthorities);
    }

    private static <T> Map<Long, T> index(Collection<T> values, Function<T, Long> id) {
        return values.stream().collect(Collectors.toMap(id, Function.identity()));
    }
}
