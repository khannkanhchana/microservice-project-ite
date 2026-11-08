package kh.edu.istad.iam.application.port.out;

import kh.edu.istad.iam.domain.Permission;
import kh.edu.istad.iam.application.query.PermissionFilterQuery;
import kh.edu.istad.iam.application.result.PageResult;
import kh.edu.istad.iam.application.result.PermissionResult;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PermissionRepository {
    Permission save(Permission permission);
    Optional<Permission> findByUuid(UUID permissionUuid);
    List<Permission> findByUuids(Collection<UUID> permissionUuids);
    boolean existsByName(String name);
    PageResult<PermissionResult> search(PermissionFilterQuery query);
}
