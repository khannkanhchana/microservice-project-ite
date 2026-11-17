package kh.edu.istad.iam.application.admin;

import kh.edu.istad.iam.application.admin.AdminModels.*;
import kh.edu.istad.iam.application.result.PageResult;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Read models and actions of the administration console that are not plain user/role/permission CRUD. */
public interface AdminConsoleUseCase {

    Overview overview();

    PageResult<AdminUserRow> users(UserQuery query);

    List<RolePermission> rolePermissions(UUID roleUuid);

    AccountSecurity accountSecurity(UUID userUuid);

    /** Clears failed attempts and any (timed) lock. */
    void unlockAccount(UUID userUuid, String actor);

    /** Emails (logs) a password reset link to the user. */
    void sendPasswordResetLink(UUID userUuid, String resetPageUrl, String actor);

    List<AdminSession> userSessions(UUID userUuid);

    void revokeUserSession(UUID userUuid, UUID sessionId, String actor);

    int revokeAllUserSessions(UUID userUuid, String actor);

    PageResult<AdminLoginAttempt> loginHistory(LoginHistoryQuery query);

    List<SuspiciousActivity> suspiciousActivity(int days);

    PageResult<AuditLogEntry> auditLogs(AuditQuery query);

    /** One audit log entry, or empty when no entry has that id. */
    Optional<AuditLogEntry> auditLog(UUID id);

    /** The most recent audit log entries (system activity), newest first; {@code limit} is clamped to 1..100. */
    List<AuditLogEntry> recentActivities(int limit);

    void audit(AuditEvent event);
}
