package kh.edu.istad.iam.application.port.out;

import kh.edu.istad.iam.domain.Identity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IdentityRepository {
    Identity save(Identity identity);
    Optional<Identity> findByUuid(UUID identityUuid);
    Optional<Identity> findByProviderAndSubject(String provider, String providerSubject);
    List<Identity> findByUserUuid(UUID userUuid);
    List<Identity> findActiveByUserUuid(UUID userUuid);
    boolean existsActiveByUserUuidAndProvider(UUID userUuid, String provider);
    int countActiveSignInMethods(UUID userUuid);
}
