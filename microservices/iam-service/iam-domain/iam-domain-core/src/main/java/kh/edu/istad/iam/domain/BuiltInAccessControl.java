package kh.edu.istad.iam.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Seed definitions for built-in roles and their user/role/permission capabilities. */
public final class BuiltInAccessControl {

    public static final String ADMIN = "ADMIN";
    public static final String USER = "USER";
    private static final List<String> RESOURCES = List.of("user", "role", "permission");
    private static final List<String> ACTIONS = List.of("create", "read", "update", "delete");

    private BuiltInAccessControl() { }

    public static SeedDefinitions definitions(Instant at, String actor) {
        if (at == null) throw new IllegalArgumentException("seed timestamp is required");
        if (actor == null || actor.isBlank()) throw new IllegalArgumentException("seed actor is required");
        String seedActor = actor.trim();

        List<Permission> permissions = new ArrayList<>();
        Map<String, Permission> permissionsByName = new LinkedHashMap<>();
        for (String resource : RESOURCES) {
            for (String action : ACTIONS) {
                String name = resource + ":" + action;
                Permission permission = new Permission(null, UUID.randomUUID(), name, resource, action,
                        "Allows " + action + " on " + resource + " resources", true, true,
                        at, seedActor, at, seedActor, false, null, null, 0);
                permissions.add(permission);
                permissionsByName.put(name, permission);
            }
        }

        Role admin = new Role(null, UUID.randomUUID(), ADMIN, "Built-in administrator role", true, true,
                at, seedActor, at, seedActor, false, null, null, 0);
        Role user = new Role(null, UUID.randomUUID(), USER, "Built-in standard user role", true, true,
                at, seedActor, at, seedActor, false, null, null, 0);

        Map<String, Set<String>> rolePermissions = new LinkedHashMap<>();
        rolePermissions.put(ADMIN, Set.copyOf(permissionsByName.keySet()));
        Set<String> readPermissions = new LinkedHashSet<>();
        for (String resource : RESOURCES) readPermissions.add(resource + ":read");
        rolePermissions.put(USER, Set.copyOf(readPermissions));

        return new SeedDefinitions(List.of(admin, user), permissions, rolePermissions);
    }

    public record SeedDefinitions(List<Role> roles, List<Permission> permissions,
                                  Map<String, Set<String>> rolePermissions) {
        public SeedDefinitions {
            roles = List.copyOf(roles);
            permissions = List.copyOf(permissions);
            Map<String, Set<String>> copy = new LinkedHashMap<>();
            rolePermissions.forEach((role, names) -> copy.put(role, Set.copyOf(names)));
            rolePermissions = Map.copyOf(copy);
        }
    }
}
