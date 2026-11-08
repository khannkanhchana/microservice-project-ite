package kh.edu.istad.iam.persistence.repository;

import kh.edu.istad.iam.persistence.entity.UserRoleAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface UserRoleAssignmentJpaRepository extends JpaRepository<UserRoleAssignment, Long> {
    @Query("select a from UserRoleAssignment a where a.user.uuid = :userUuid")
    List<UserRoleAssignment> findByUserUuid(@Param("userUuid") UUID userUuid);

    @Query("select a from UserRoleAssignment a where a.user.uuid = :userUuid and a.role.uuid = :roleUuid "
            + "and a.assignedAt <= :at and (a.expiresAt is null or a.expiresAt > :at) "
            + "and (a.revokedAt is null or a.revokedAt > :at)")
    List<UserRoleAssignment> findActive(@Param("userUuid") UUID userUuid,
                                        @Param("roleUuid") UUID roleUuid,
                                        @Param("at") Instant at);
}
