package kh.edu.istad.iam.application.admin;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Commands, queries and results of the administration console. */
public final class AdminModels {
    private AdminModels() { }

    // ---- Overview ---------------------------------------------------------------------------------------------

    public record Overview(long totalUsers, long activeUsers, long disabledUsers, long lockedUsers,
                           long usersSignedInNow, long newUsersLast7Days, long roles, long permissions, long clients,
                           long successfulLogins24h, long failedLogins24h, long suspiciousEvents7Days,
                           List<DailyLogins> loginsLast7Days, List<AuditLogEntry> recentActivities) {
        public Overview {
            loginsLast7Days = List.copyOf(loginsLast7Days);
            recentActivities = List.copyOf(recentActivities);
        }
    }

    public record DailyLogins(String date, long successful, long failed) { }

    // ---- Users ------------------------------------------------------------------------------------------------

    /** status: all | active | disabled | locked. */
    public record UserQuery(String search, String status, UUID roleUuid, int page, int size) { }

    public record AdminUserRow(UUID uuid, String username, String name, String email, String pictureUrl,
                               boolean enabled, boolean locked, boolean emailVerified, List<String> roles,
                               int failedLoginAttempts, Instant lastLoginAt, Instant createdAt) {
        public AdminUserRow { roles = List.copyOf(roles); }
    }

    public record AccountSecurity(UUID userUuid, boolean enabled, boolean locked, Instant lockExpiresAt,
                                  int failedLoginAttempts, boolean hasPassword, Instant passwordChangedAt,
                                  Instant lastLoginAt, long activeSessions, long failedLogins24h,
                                  List<String> linkedProviders) {
        public AccountSecurity { linkedProviders = List.copyOf(linkedProviders); }
    }

    public record RolePermission(UUID uuid, String name, String resource, String action, String description,
                                 Instant assignedAt, String assignedBy) { }

    // ---- Sessions & login monitoring --------------------------------------------------------------------------

    public record AdminSession(UUID id, UUID userUuid, String username, String loginMethod, String browser, String os,
                               String deviceType, String ipAddress, String location, Instant createdAt,
                               Instant lastActivityAt) { }

    /** status: all | success | failed. */
    public record LoginHistoryQuery(UUID userUuid, String search, String status, String method, String ipAddress,
                                    Instant from, Instant to, int page, int size) { }

    public record AdminLoginAttempt(UUID id, UUID userUuid, String username, String identifier, boolean success,
                                    String method, String failureReason, String ipAddress, String browser, String os,
                                    String deviceType, String location, Instant attemptedAt) { }

    /** type: BRUTE_FORCE | PASSWORD_SPRAY | SUCCESS_AFTER_FAILURES | LOCKED_ACCOUNT_ATTEMPTS. */
    public record SuspiciousActivity(String type, String severity, String subject, UUID userUuid, String ipAddress,
                                     long attempts, long distinctAccounts, Instant firstSeen, Instant lastSeen) { }

    // ---- Audit log --------------------------------------------------------------------------------------------

    public record AuditEvent(String category, String action, String outcome, String actor, UUID actorUuid,
                             String targetType, String targetId, String description, String details,
                             String ipAddress, String userAgent, String httpMethod, String path, Integer status) { }

    public record AuditQuery(String category, String action, String outcome, String actor, String target,
                             String search, Instant from, Instant to, int page, int size) { }

    public record AuditLogEntry(UUID id, Instant occurredAt, String category, String action, String outcome,
                                String actor, UUID actorUuid, String targetType, String targetId, String description,
                                String details, String ipAddress, String httpMethod, String path, Integer status) { }

    // ---- OAuth2 clients ---------------------------------------------------------------------------------------

    public record ClientCommand(String clientId, String clientName, List<String> authenticationMethods,
                                List<String> grantTypes, List<String> redirectUris, List<String> postLogoutRedirectUris,
                                List<String> scopes, Boolean requireProofKey, Boolean requireConsent,
                                Integer accessTokenMinutes, Integer refreshTokenDays, Boolean reuseRefreshTokens) {
        public ClientCommand {
            authenticationMethods = authenticationMethods == null ? List.of() : List.copyOf(authenticationMethods);
            grantTypes = grantTypes == null ? List.of() : List.copyOf(grantTypes);
            redirectUris = redirectUris == null ? List.of() : List.copyOf(redirectUris);
            postLogoutRedirectUris = postLogoutRedirectUris == null ? List.of() : List.copyOf(postLogoutRedirectUris);
            scopes = scopes == null ? List.of() : List.copyOf(scopes);
        }
    }

    public record ClientDetails(String id, String clientId, String clientName, Instant clientIdIssuedAt,
                                boolean confidential, Instant clientSecretExpiresAt, List<String> authenticationMethods,
                                List<String> grantTypes, List<String> redirectUris, List<String> postLogoutRedirectUris,
                                List<String> scopes, boolean requireProofKey, boolean requireConsent,
                                long accessTokenMinutes, long refreshTokenDays, boolean reuseRefreshTokens,
                                long authorizationCodeMinutes, boolean system, long activeAuthorizations) { }

    /** Returned on create and secret rotation only: the plain secret is never stored or shown again. */
    public record ClientWithSecret(ClientDetails client, String clientSecret) { }

    // ---- System settings --------------------------------------------------------------------------------------

    public record AuthenticationPolicy(boolean allowSelfRegistration, int maxFailedAttempts, int lockoutMinutes,
                                       List<String> enabledSocialProviders) {
        public AuthenticationPolicy {
            enabledSocialProviders = enabledSocialProviders == null ? List.of() : List.copyOf(enabledSocialProviders);
        }
    }

    public record PasswordPolicy(int minLength, boolean requireUppercase, boolean requireLowercase,
                                 boolean requireDigit, boolean requireSpecial, int maxAgeDays) { }

    public record SessionPolicy(int idleTimeoutMinutes) { }

    public record TokenPolicy(int accessTokenMinutes, int refreshTokenDays, int authorizationCodeMinutes) { }

    public record SystemSettings(AuthenticationPolicy authentication, PasswordPolicy password, SessionPolicy session,
                                 TokenPolicy tokens, Instant updatedAt, String updatedBy) { }

    /** Generic key/value view used for audit details. */
    public static Map<String, Object> details(Object... keyValues) {
        java.util.LinkedHashMap<String, Object> map = new java.util.LinkedHashMap<>();
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            if (keyValues[i + 1] != null) map.put(String.valueOf(keyValues[i]), keyValues[i + 1]);
        }
        return map;
    }
}
