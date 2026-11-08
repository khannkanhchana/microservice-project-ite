package kh.edu.istad.iam.persistence.repository;

import kh.edu.istad.iam.persistence.entity.Identity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IdentityJpaRepository extends JpaRepository<Identity, Long> {
    Optional<Identity> findByUuid(UUID uuid);
    Optional<Identity> findByProviderAndProviderSubject(String provider, String providerSubject);
    List<Identity> findByUser_Uuid(UUID userUuid);
    List<Identity> findByUser_UuidAndDeletedFalse(UUID userUuid);
    boolean existsByUser_UuidAndProviderAndDeletedFalse(UUID userUuid, String provider);
    long countByUser_IdAndDeletedFalse(Long userId);
}
