package kh.edu.istad.iam.persistence.repository;

import kh.edu.istad.iam.persistence.entity.UserPermissionAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface UserPermissionAssignmentJpaRepository extends JpaRepository<UserPermissionAssignment, Long> {
    @Query("select a from UserPermissionAssignment a where a.user.uuid = :userUuid")
    List<UserPermissionAssignment> findByUserUuid(@Param("userUuid") UUID userUuid);

    @Query("select a from UserPermissionAssignment a where a.user.uuid = :userUuid and a.permission.uuid = :permissionUuid "
            + "and a.assignedAt <= :at and (a.expiresAt is null or a.expiresAt > :at) "
            + "and (a.revokedAt is null or a.revokedAt > :at)")
    List<UserPermissionAssignment> findActive(@Param("userUuid") UUID userUuid,
                                              @Param("permissionUuid") UUID permissionUuid,
                                              @Param("at") Instant at);
}
