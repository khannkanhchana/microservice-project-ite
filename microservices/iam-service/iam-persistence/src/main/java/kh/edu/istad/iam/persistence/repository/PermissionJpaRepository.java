package kh.edu.istad.iam.persistence.repository;

import kh.edu.istad.iam.persistence.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PermissionJpaRepository extends JpaRepository<Permission, Long>, JpaSpecificationExecutor<Permission> {
    Optional<Permission> findByUuid(UUID uuid);
    List<Permission> findByUuidIn(Collection<UUID> uuids);
    boolean existsByNameIgnoreCase(String name);
}
