package kh.edu.istad.iam.persistence.adapter;

import kh.edu.istad.iam.application.port.out.RoleRepository;
import kh.edu.istad.iam.application.query.RoleFilterQuery;
import kh.edu.istad.iam.application.query.SortDirection;
import kh.edu.istad.iam.application.result.PageResult;
import kh.edu.istad.iam.application.result.RoleSummary;
import kh.edu.istad.iam.domain.Role;
import kh.edu.istad.iam.persistence.repository.RoleJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional
public class RolePersistenceAdapter implements RoleRepository {
    private final RoleJpaRepository repository;

    public RolePersistenceAdapter(RoleJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Role save(Role role) {
        return toDomain(repository.save(toEntity(role)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Role> findByUuid(UUID roleUuid) {
        return repository.findByUuid(roleUuid).map(RolePersistenceAdapter::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Role> findByUuids(Collection<UUID> roleUuids) {
        if (roleUuids.isEmpty()) return List.of();
        return repository.findByUuidIn(roleUuids).stream().map(RolePersistenceAdapter::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Role> findByIds(Collection<Long> roleIds) {
        if (roleIds.isEmpty()) return List.of();
        return repository.findAllById(roleIds).stream().map(RolePersistenceAdapter::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByName(String name) {
        return repository.existsByNameIgnoreCase(name);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<RoleSummary> search(RoleFilterQuery query) {
        Specification<kh.edu.istad.iam.persistence.entity.Role> specification = (root, cq, cb) -> cb.conjunction();
        if (query.system() != null) specification = specification.and((root, cq, cb) -> cb.equal(root.get("system"), query.system()));
        if (query.active() != null) specification = specification.and((root, cq, cb) -> cb.equal(root.get("active"), query.active()));
        if (query.deleted() != null) specification = specification.and((root, cq, cb) -> cb.equal(root.get("deleted"), query.deleted()));
        if (query.search() != null && !query.search().isBlank()) {
            String pattern = "%" + query.search().trim().toLowerCase(java.util.Locale.ROOT) + "%";
            specification = specification.and((root, cq, cb) -> cb.or(
                    cb.like(cb.lower(root.get("name")), pattern),
                    cb.like(cb.lower(root.get("description")), pattern)));
        }
        Sort sort = Sort.by(query.sortDirection() == SortDirection.DESC ? Sort.Direction.DESC : Sort.Direction.ASC,
                query.sortBy().equals("createdAt") ? "createdAt" : "name");
        Page<kh.edu.istad.iam.persistence.entity.Role> page = repository.findAll(specification,
                PageRequest.of(query.page(), query.size(), sort));
        List<RoleSummary> content = page.getContent().stream().map(entity -> new RoleSummary(
                entity.getUuid(), entity.getName(), entity.getDescription(), entity.isSystem(),
                entity.isActive() && !entity.isDeleted(), entity.getCreatedAt())).toList();
        return new PageResult<>(content, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    private static kh.edu.istad.iam.persistence.entity.Role toEntity(Role role) {
        var entity = new kh.edu.istad.iam.persistence.entity.Role();
        entity.setId(role.id()); entity.setUuid(role.uuid()); entity.setName(role.name());
        entity.setDescription(role.description()); entity.setSystem(role.system()); entity.setActive(role.active());
        entity.setCreatedAt(role.createdAt()); entity.setCreatedBy(role.createdBy());
        entity.setUpdatedAt(role.updatedAt()); entity.setUpdatedBy(role.updatedBy());
        entity.setDeleted(role.deleted()); entity.setDeletedAt(role.deletedAt()); entity.setDeletedBy(role.deletedBy());
        entity.setVersion(role.version());
        return entity;
    }

    private static Role toDomain(kh.edu.istad.iam.persistence.entity.Role entity) {
        return new Role(entity.getId(), entity.getUuid(), entity.getName(), entity.getDescription(),
                entity.isSystem(), entity.isActive(), entity.getCreatedAt(), entity.getCreatedBy(),
                entity.getUpdatedAt(), entity.getUpdatedBy(), entity.isDeleted(), entity.getDeletedAt(),
                entity.getDeletedBy(), entity.getVersion());
    }
}
