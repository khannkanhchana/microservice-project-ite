package kh.edu.istad.iam.persistence.adapter;

import kh.edu.istad.iam.application.port.out.PermissionRepository;
import kh.edu.istad.iam.application.query.PermissionFilterQuery;
import kh.edu.istad.iam.application.query.SortDirection;
import kh.edu.istad.iam.application.result.PageResult;
import kh.edu.istad.iam.application.result.PermissionResult;
import kh.edu.istad.iam.domain.Permission;
import kh.edu.istad.iam.persistence.repository.PermissionJpaRepository;
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
public class PermissionPersistenceAdapter implements PermissionRepository {
    private final PermissionJpaRepository repository;

    public PermissionPersistenceAdapter(PermissionJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Permission save(Permission permission) {
        return toDomain(repository.save(toEntity(permission)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Permission> findByUuid(UUID permissionUuid) {
        return repository.findByUuid(permissionUuid).map(PermissionPersistenceAdapter::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Permission> findByUuids(Collection<UUID> permissionUuids) {
        if (permissionUuids.isEmpty()) return List.of();
        return repository.findByUuidIn(permissionUuids).stream().map(PermissionPersistenceAdapter::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Permission> findByIds(Collection<Long> permissionIds) {
        if (permissionIds.isEmpty()) return List.of();
        return repository.findAllById(permissionIds).stream().map(PermissionPersistenceAdapter::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByName(String name) {
        return repository.existsByNameIgnoreCase(name);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<PermissionResult> search(PermissionFilterQuery query) {
        Specification<kh.edu.istad.iam.persistence.entity.Permission> specification = (root, cq, cb) -> cb.conjunction();
        if (query.system() != null) specification = specification.and((root, cq, cb) -> cb.equal(root.get("system"), query.system()));
        if (query.active() != null) specification = specification.and((root, cq, cb) -> cb.equal(root.get("active"), query.active()));
        if (query.deleted() != null) specification = specification.and((root, cq, cb) -> cb.equal(root.get("deleted"), query.deleted()));
        if (query.resource() != null && !query.resource().isBlank())
            specification = specification.and((root, cq, cb) -> cb.equal(cb.lower(root.get("resource")), query.resource().trim().toLowerCase(java.util.Locale.ROOT)));
        if (query.action() != null && !query.action().isBlank())
            specification = specification.and((root, cq, cb) -> cb.equal(cb.lower(root.get("action")), query.action().trim().toLowerCase(java.util.Locale.ROOT)));
        if (query.search() != null && !query.search().isBlank()) {
            String pattern = "%" + query.search().trim().toLowerCase(java.util.Locale.ROOT) + "%";
            specification = specification.and((root, cq, cb) -> cb.or(
                    cb.like(cb.lower(root.get("name")), pattern),
                    cb.like(cb.lower(root.get("resource")), pattern),
                    cb.like(cb.lower(root.get("action")), pattern),
                    cb.like(cb.lower(root.get("description")), pattern)));
        }
        String sortProperty = switch (query.sortBy()) {
            case "createdAt" -> "createdAt";
            case "resource" -> "resource";
            case "action" -> "action";
            default -> "name";
        };
        Sort sort = Sort.by(query.sortDirection() == SortDirection.DESC ? Sort.Direction.DESC : Sort.Direction.ASC, sortProperty);
        Page<kh.edu.istad.iam.persistence.entity.Permission> page = repository.findAll(specification,
                PageRequest.of(query.page(), query.size(), sort));
        List<PermissionResult> content = page.getContent().stream().map(PermissionPersistenceAdapter::toResult).toList();
        return new PageResult<>(content, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    private static kh.edu.istad.iam.persistence.entity.Permission toEntity(Permission permission) {
        var entity = new kh.edu.istad.iam.persistence.entity.Permission();
        entity.setId(permission.id()); entity.setUuid(permission.uuid()); entity.setName(permission.name());
        entity.setResource(permission.resource()); entity.setAction(permission.action()); entity.setDescription(permission.description());
        entity.setSystem(permission.system()); entity.setActive(permission.active());
        entity.setCreatedAt(permission.createdAt()); entity.setCreatedBy(permission.createdBy());
        entity.setUpdatedAt(permission.updatedAt()); entity.setUpdatedBy(permission.updatedBy());
        entity.setDeleted(permission.deleted()); entity.setDeletedAt(permission.deletedAt()); entity.setDeletedBy(permission.deletedBy());
        entity.setVersion(permission.version());
        return entity;
    }

    private static Permission toDomain(kh.edu.istad.iam.persistence.entity.Permission entity) {
        return new Permission(entity.getId(), entity.getUuid(), entity.getName(), entity.getResource(), entity.getAction(),
                entity.getDescription(), entity.isSystem(), entity.isActive(), entity.getCreatedAt(), entity.getCreatedBy(),
                entity.getUpdatedAt(), entity.getUpdatedBy(), entity.isDeleted(), entity.getDeletedAt(),
                entity.getDeletedBy(), entity.getVersion());
    }

    private static PermissionResult toResult(kh.edu.istad.iam.persistence.entity.Permission entity) {
        return new PermissionResult(entity.getUuid(), entity.getName(), entity.getResource(), entity.getAction(),
                entity.getDescription(), entity.isSystem(), entity.isActive() && !entity.isDeleted(), entity.isDeleted(),
                entity.getCreatedAt(), entity.getCreatedBy(), entity.getUpdatedAt(), entity.getUpdatedBy(),
                entity.getDeletedAt(), entity.getDeletedBy(), entity.getVersion());
    }
}
