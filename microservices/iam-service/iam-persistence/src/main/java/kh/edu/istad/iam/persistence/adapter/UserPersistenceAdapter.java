package kh.edu.istad.iam.persistence.adapter;

import kh.edu.istad.iam.application.port.out.UserRepository;
import kh.edu.istad.iam.application.query.UserFilterQuery;
import kh.edu.istad.iam.application.query.SortDirection;
import kh.edu.istad.iam.application.result.PageResult;
import kh.edu.istad.iam.application.result.UserSummary;
import kh.edu.istad.iam.domain.User;
import kh.edu.istad.iam.persistence.repository.UserJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
@Transactional
public class UserPersistenceAdapter implements UserRepository {
    private final UserJpaRepository repository;

    public UserPersistenceAdapter(UserJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public User save(User user) {
        var entity = toEntity(user);
        if (entity.getId() != null) {
            // Domain objects carry the version after their changes; JPA @Version expects the version as loaded.
            long current = repository.findById(entity.getId())
                    .orElseThrow(() -> new IllegalArgumentException("User does not exist"))
                    .getVersion();
            if (user.getVersion() <= current) {
                throw new org.springframework.orm.ObjectOptimisticLockingFailureException(
                        kh.edu.istad.iam.persistence.entity.User.class, entity.getId());
            }
            entity.setVersion(current);
        }
        return toDomain(repository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findById(Long id) {
        return repository.findById(id).map(UserPersistenceAdapter::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByUuid(UUID userUuid) {
        return repository.findByUuid(userUuid).map(UserPersistenceAdapter::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByUsernameOrEmail(String usernameOrEmail) {
        return repository.findFirstByUsernameIgnoreCaseOrEmailIgnoreCase(usernameOrEmail, usernameOrEmail)
                .map(UserPersistenceAdapter::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return repository.existsByUsernameIgnoreCase(username);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return repository.existsByEmailIgnoreCase(email.toLowerCase(java.util.Locale.ROOT));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<UserSummary> search(UserFilterQuery query) {
        Specification<kh.edu.istad.iam.persistence.entity.User> specification = (root, cq, cb) -> cb.conjunction();
        if (query.enabled() != null) specification = specification.and((root, cq, cb) -> cb.equal(root.get("enabled"), query.enabled()));
        if (query.deleted() != null) specification = specification.and((root, cq, cb) -> cb.equal(root.get("deleted"), query.deleted()));
        if (query.search() != null && !query.search().isBlank()) {
            String pattern = "%" + query.search().trim().toLowerCase(java.util.Locale.ROOT) + "%";
            specification = specification.and((root, cq, cb) -> cb.or(
                    cb.like(cb.lower(root.get("username")), pattern),
                    cb.like(cb.lower(root.get("email")), pattern),
                    cb.like(cb.lower(root.get("givenName")), pattern),
                    cb.like(cb.lower(root.get("familyName")), pattern)));
        }
        Sort sort = Sort.by(query.sortDirection() == SortDirection.DESC
                ? Sort.Direction.DESC : Sort.Direction.ASC, safeSortProperty(query.sortBy()));
        Page<kh.edu.istad.iam.persistence.entity.User> page = repository.findAll(specification,
                PageRequest.of(query.page(), query.size(), sort));
        return new PageResult<>(page.getContent().stream().map(UserPersistenceAdapter::toSummary).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    private String safeSortProperty(String property) {
        return switch (property) {
            case "username", "email", "givenName", "createdAt" -> property;
            default -> "createdAt";
        };
    }

    private static kh.edu.istad.iam.persistence.entity.User toEntity(User user) {
        var entity = new kh.edu.istad.iam.persistence.entity.User();
        entity.setId(user.getId());
        entity.setUuid(user.getUuid());
        entity.setUsername(user.getUsername());
        entity.setPasswordHash(user.getPasswordHash());
        entity.setGivenName(user.getGivenName());
        entity.setFamilyName(user.getFamilyName());
        entity.setMiddleName(user.getMiddleName());
        entity.setNickname(user.getNickname());
        entity.setGender(user.getGender());
        entity.setBirthdate(user.getBirthdate());
        entity.setLocale(user.getLocale());
        entity.setProfileUrl(user.getProfileUrl());
        entity.setPictureUrl(user.getPictureUrl());
        entity.setWebsiteUrl(user.getWebsiteUrl());
        entity.setEmail(user.getEmail());
        entity.setEmailVerified(user.isEmailVerified());
        entity.setPhoneNumber(user.getPhoneNumber());
        entity.setPhoneNumberVerified(user.isPhoneNumberVerified());
        entity.setZoneinfo(user.getZoneinfo());
        entity.setAddressStreetAddress(user.getAddressStreetAddress());
        entity.setAddressLocality(user.getAddressLocality());
        entity.setAddressRegion(user.getAddressRegion());
        entity.setAddressPostalCode(user.getAddressPostalCode());
        entity.setAddressCountry(user.getAddressCountry());
        entity.setEnabled(user.isEnabledFlag());
        entity.setAccountNonExpired(user.isAccountNonExpired());
        entity.setAccountNonLocked(user.isAccountNonLocked());
        entity.setCredentialsNonExpired(user.isCredentialsNonExpired());
        entity.setFailedLoginAttempts(user.getFailedLoginAttempts());
        entity.setLastLoginAt(user.getLastLoginAt());
        entity.setPasswordChangedAt(user.getPasswordChangedAt());
        entity.setCreatedAt(user.getCreatedAt());
        entity.setCreatedBy(user.getCreatedBy());
        entity.setUpdatedAt(user.getUpdatedAt() == null ? user.getCreatedAt() : user.getUpdatedAt());
        entity.setUpdatedBy(user.getUpdatedBy() == null ? user.getCreatedBy() : user.getUpdatedBy());
        entity.setDeleted(user.isDeleted());
        entity.setDeletedAt(user.getDeletedAt());
        entity.setDeletedBy(user.getDeletedBy());
        entity.setVersion(user.getVersion());
        return entity;
    }

    private static User toDomain(kh.edu.istad.iam.persistence.entity.User entity) {
        return User.builder()
                .id(entity.getId()).uuid(entity.getUuid()).username(entity.getUsername()).passwordHash(entity.getPasswordHash())
                .givenName(entity.getGivenName()).familyName(entity.getFamilyName()).middleName(entity.getMiddleName())
                .nickname(entity.getNickname()).gender(entity.getGender()).birthdate(entity.getBirthdate())
                .locale(entity.getLocale()).profileUrl(entity.getProfileUrl()).pictureUrl(entity.getPictureUrl())
                .websiteUrl(entity.getWebsiteUrl()).email(entity.getEmail()).emailVerified(entity.isEmailVerified())
                .phoneNumber(entity.getPhoneNumber()).phoneNumberVerified(entity.isPhoneNumberVerified())
                .zoneinfo(entity.getZoneinfo()).addressStreetAddress(entity.getAddressStreetAddress())
                .addressLocality(entity.getAddressLocality()).addressRegion(entity.getAddressRegion())
                .addressPostalCode(entity.getAddressPostalCode()).addressCountry(entity.getAddressCountry())
                .enabled(entity.isEnabled()).accountNonExpired(entity.isAccountNonExpired())
                .accountNonLocked(entity.isAccountNonLocked()).credentialsNonExpired(entity.isCredentialsNonExpired())
                .failedLoginAttempts(entity.getFailedLoginAttempts()).lastLoginAt(entity.getLastLoginAt())
                .passwordChangedAt(entity.getPasswordChangedAt()).createdAt(entity.getCreatedAt()).createdBy(entity.getCreatedBy())
                .updatedAt(entity.getUpdatedAt()).updatedBy(entity.getUpdatedBy()).deleted(entity.isDeleted())
                .deletedAt(entity.getDeletedAt()).deletedBy(entity.getDeletedBy()).version(entity.getVersion())
                .authorities(Set.of()).build();
    }

    private static UserSummary toSummary(kh.edu.istad.iam.persistence.entity.User entity) {
        String name = entity.getMiddleName() == null
                ? entity.getGivenName() + " " + entity.getFamilyName()
                : entity.getGivenName() + " " + entity.getMiddleName() + " " + entity.getFamilyName();
        return new UserSummary(entity.getUuid(), entity.getUsername(), name, entity.getEmail(),
                entity.isEmailVerified(), entity.isEnabled() && !entity.isDeleted(), entity.getCreatedAt());
    }
}
