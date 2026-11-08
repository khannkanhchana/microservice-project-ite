package kh.edu.istad.iam.persistence.repository;

import kh.edu.istad.iam.persistence.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoleJpaRepository extends JpaRepository<Role, Long>, JpaSpecificationExecutor<Role> {
    Optional<Role> findByUuid(UUID uuid);
    List<Role> findByUuidIn(Collection<UUID> uuids);
    boolean existsByNameIgnoreCase(String name);
}
