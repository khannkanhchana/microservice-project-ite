package kh.edu.istad.iam.application.port.out;

import kh.edu.istad.iam.application.query.RoleFilterQuery;
import kh.edu.istad.iam.application.result.PageResult;
import kh.edu.istad.iam.application.result.RoleSummary;
import kh.edu.istad.iam.domain.Role;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoleRepository {
    Role save(Role role);
    Optional<Role> findByUuid(UUID roleUuid);
    List<Role> findByUuids(Collection<UUID> roleUuids);
    List<Role> findByIds(Collection<Long> roleIds);
    boolean existsByName(String name);
    PageResult<RoleSummary> search(RoleFilterQuery query);
}
