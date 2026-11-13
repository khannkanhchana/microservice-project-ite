package kh.edu.istad.iam.application.admin;

import kh.edu.istad.iam.application.admin.AdminModels.*;
import kh.edu.istad.iam.application.result.PageResult;

import java.util.List;
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

    void audit(AuditEvent event);
}
