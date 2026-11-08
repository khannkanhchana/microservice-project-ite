package kh.edu.istad.iam.domain;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Resolves Spring Security authority names from active, non-deleted assignment records. */
public final class IamAuthorizationPolicy {

    private IamAuthorizationPolicy() { }

    public static Set<String> resolveAuthorities(
            User user,
            Collection<Role> roles,
            Collection<Permission> permissions,
            Collection<UserRoleAssignment> userRoleAssignments,
            Collection<RolePermissionAssignment> rolePermissionAssignments,
            Collection<UserPermissionAssignment> userPermissionAssignments,
            Instant at) {
        Objects.requireNonNull(user, "user is required");
        Objects.requireNonNull(at, "evaluation time is required");
        if (!user.isEnabled() || !user.isAccountNonExpired() || !user.isAccountNonLocked()
                || !user.isCredentialsNonExpired()) return Set.of();

        Map<Long, Role> rolesById = index(roles, Role::id);
        Map<Long, Permission> permissionsById = index(permissions, Permission::id);
        Set<Long> activeRoleIds = new HashSet<>();
        Set<String> authorities = new LinkedHashSet<>();

        for (UserRoleAssignment assignment : userRoleAssignments) {
            if (assignment.getUserId().equals(user.getId()) && assignment.isActiveAt(at)) {
                Role role = rolesById.get(assignment.getRoleId());
                if (isActive(role)) {
                    activeRoleIds.add(role.id());
                    authorities.add("ROLE_" + role.name());
                }
            }
        }

        Set<String> effectivePermissions = new HashSet<>();
        for (RolePermissionAssignment assignment : rolePermissionAssignments) {
            if (activeRoleIds.contains(assignment.getRoleId()) && assignment.isActiveAt(at)) {
                Permission permission = permissionsById.get(assignment.getPermissionId());
                if (isActive(permission)) effectivePermissions.add(permission.name());
            }
        }

        Set<String> directGrants = new HashSet<>();
        Set<String> directDenials = new HashSet<>();
        for (UserPermissionAssignment assignment : userPermissionAssignments) {
            if (assignment.getUserId().equals(user.getId()) && assignment.isActiveAt(at)) {
                Permission permission = permissionsById.get(assignment.getPermissionId());
                if (isActive(permission)) {
                    if (assignment.getEffect() == AssignmentEffect.GRANT) directGrants.add(permission.name());
                    else directDenials.add(permission.name());
                }
            }
        }

        effectivePermissions.addAll(directGrants);
        effectivePermissions.removeAll(directDenials);
        authorities.addAll(effectivePermissions);
        return Collections.unmodifiableSet(authorities);
    }

    private static boolean isActive(Role role) {
        return role != null && role.active() && !role.deleted();
    }

    private static boolean isActive(Permission permission) {
        return permission != null && permission.active() && !permission.deleted();
    }

    private static <T> Map<Long, T> index(Collection<T> values, Function<T, Long> idFunction) {
        Objects.requireNonNull(values, "model collection is required");
        Map<Long, T> indexed = new HashMap<>();
        for (T value : values) {
            Objects.requireNonNull(value, "model collection cannot contain null");
            Long id = idFunction.apply(value);
            if (id != null) indexed.put(id, value);
        }
        return indexed;
    }
}
