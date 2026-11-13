package kh.edu.istad.iam.persistence.adapter;

import kh.edu.istad.iam.application.port.out.IdentityRepository;
import kh.edu.istad.iam.domain.Identity;
import kh.edu.istad.iam.persistence.entity.User;
import kh.edu.istad.iam.persistence.repository.IdentityJpaRepository;
import kh.edu.istad.iam.persistence.repository.UserJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional
public class IdentityPersistenceAdapter implements IdentityRepository {
    private final IdentityJpaRepository identities;
    private final UserJpaRepository users;

    public IdentityPersistenceAdapter(IdentityJpaRepository identities, UserJpaRepository users) {
        this.identities = identities;
        this.users = users;
    }

    @Override
    public Identity save(Identity identity) {
        var entity = toEntity(identity);
        if (entity.getId() != null) {
            // Domain objects carry the version after their changes; JPA @Version expects the version as loaded.
            long current = identities.findById(entity.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Identity does not exist"))
                    .getVersion();
            if (identity.version() <= current) {
                throw new org.springframework.orm.ObjectOptimisticLockingFailureException(
                        kh.edu.istad.iam.persistence.entity.Identity.class, entity.getId());
            }
            entity.setVersion(current);
        }
        return toDomain(identities.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Identity> findByUuid(UUID identityUuid) {
        return identities.findByUuid(identityUuid).map(IdentityPersistenceAdapter::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Identity> findByProviderAndSubject(String provider, String providerSubject) {
        return identities.findByProviderAndProviderSubject(provider.toUpperCase(java.util.Locale.ROOT), providerSubject)
                .map(IdentityPersistenceAdapter::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Identity> findByUserUuid(UUID userUuid) {
        return identities.findByUser_Uuid(userUuid).stream().map(IdentityPersistenceAdapter::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Identity> findActiveByUserUuid(UUID userUuid) {
        return identities.findByUser_UuidAndDeletedFalse(userUuid).stream().map(IdentityPersistenceAdapter::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsActiveByUserUuidAndProvider(UUID userUuid, String provider) {
        return identities.existsByUser_UuidAndProviderAndDeletedFalse(userUuid, provider.toUpperCase(java.util.Locale.ROOT));
    }

    @Override
    @Transactional(readOnly = true)
    public int countActiveSignInMethods(UUID userUuid) {
        Optional<User> user = users.findByUuid(userUuid);
        if (user.isEmpty() || user.get().isDeleted()) return 0;
        long socialMethods = identities.countByUser_IdAndDeletedFalse(user.get().getId());
        return Math.toIntExact(socialMethods + (user.get().getPasswordHash() == null ? 0 : 1));
    }

    private kh.edu.istad.iam.persistence.entity.Identity toEntity(Identity identity) {
        var entity = new kh.edu.istad.iam.persistence.entity.Identity();
        entity.setId(identity.id());
        entity.setUuid(identity.uuid());
        entity.setUser(users.findById(identity.userId()).orElseThrow(() -> new IllegalArgumentException("Identity user does not exist")));
        entity.setProvider(identity.provider());
        entity.setProviderSubject(identity.providerSubject());
        entity.setEmail(identity.email());
        entity.setEmailVerified(identity.emailVerified());
        entity.setDisplayName(identity.displayName());
        entity.setUsername(identity.username());
        entity.setPictureUrl(identity.pictureUrl());
        entity.setAttributes(identity.attributes());
        entity.setLinkedAt(identity.linkedAt());
        entity.setLastLoginAt(identity.lastLoginAt());
        entity.setCreatedAt(identity.createdAt());
        entity.setCreatedBy(identity.createdBy());
        entity.setUpdatedAt(identity.updatedAt());
        entity.setUpdatedBy(identity.updatedBy());
        entity.setDeleted(identity.deleted());
        entity.setDeletedAt(identity.deletedAt());
        entity.setDeletedBy(identity.deletedBy());
        entity.setVersion(identity.version());
        return entity;
    }

    private static Identity toDomain(kh.edu.istad.iam.persistence.entity.Identity entity) {
        return new Identity(entity.getId(), entity.getUuid(), entity.getUser().getId(), entity.getProvider(),
                entity.getProviderSubject(), entity.getEmail(), entity.isEmailVerified(), entity.getDisplayName(),
                entity.getUsername(), entity.getPictureUrl(), entity.getAttributes(), entity.getLinkedAt(),
                entity.getLastLoginAt(), entity.getCreatedAt(), entity.getCreatedBy(), entity.getUpdatedAt(),
                entity.getUpdatedBy(), entity.isDeleted(), entity.getDeletedAt(), entity.getDeletedBy(), entity.getVersion());
    }
}
