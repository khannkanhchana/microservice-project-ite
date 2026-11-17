package kh.edu.istad.iam.restapi;

import kh.edu.istad.iam.application.admin.AdminConsoleUseCase;
import kh.edu.istad.iam.application.admin.AdminModels.*;
import kh.edu.istad.iam.application.result.PageResult;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Administration console API (under /api/admin; user sessions also at /api/users/{uuid}/sessions): overview, user account security, user sessions, login monitoring and audit logs.
 * User/role/permission CRUD and assignments live in UserController, RoleController and PermissionController.
 */
@RestController
@RequestMapping("/api")
public class AdminController {
    private final ObjectProvider<AdminConsoleUseCase> console;
    private final String webBaseUrl;

    public AdminController(ObjectProvider<AdminConsoleUseCase> console,
                           @Value("${iam.web.base-url:http://localhost:9000}") String webBaseUrl) {
        this.console = console;
        this.webBaseUrl = webBaseUrl.endsWith("/") ? webBaseUrl.substring(0, webBaseUrl.length() - 1) : webBaseUrl;
    }

    @GetMapping({"/admin/overview", "/admin/dashboard/overview"})
    @PreAuthorize("hasAuthority('dashboard:read')")
    public Overview overview() {
        return service().overview();
    }

    /** Recent system activity (newest audit log entries) for the dashboard feed. */
    @GetMapping("/admin/dashboard/activities")
    @PreAuthorize("hasAuthority('dashboard:read')")
    public List<AuditLogEntry> recentActivities(@RequestParam(name = "limit", defaultValue = "20") int limit) {
        return service().recentActivities(limit);
    }

    // ---- Users ------------------------------------------------------------------------------------------------

    @GetMapping("/admin/users")
    @PreAuthorize("hasAuthority('user:read')")
    public PageResult<AdminUserRow> users(@RequestParam(name = "search", required = false) String search,
                                          @RequestParam(name = "status", defaultValue = "all") String status,
                                          @RequestParam(name = "role", required = false) UUID role,
                                          @RequestParam(name = "page", defaultValue = "0") int page,
                                          @RequestParam(name = "size", defaultValue = "20") int size) {
        return service().users(new UserQuery(search, status, role, page, size));
    }

    @GetMapping("/admin/roles/{uuid}/permissions")
    @PreAuthorize("hasAuthority('role:read')")
    public List<RolePermission> rolePermissions(@PathVariable("uuid") UUID uuid) {
        return service().rolePermissions(uuid);
    }

    @GetMapping("/admin/users/{uuid}/security")
    @PreAuthorize("hasAuthority('user:read')")
    public AccountSecurity security(@PathVariable("uuid") UUID uuid) {
        return service().accountSecurity(uuid);
    }

    @PostMapping("/admin/users/{uuid}/unlock")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('user:update')")
    public void unlock(@PathVariable("uuid") UUID uuid, Authentication auth) {
        service().unlockAccount(uuid, auth.getName());
    }

    @PostMapping("/admin/users/{uuid}/password-reset-link")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasAuthority('user:update')")
    public void sendPasswordResetLink(@PathVariable("uuid") UUID uuid, Authentication auth) {
        service().sendPasswordResetLink(uuid, webBaseUrl + "/reset-password", auth.getName());
    }

    // ---- Sessions ---------------------------------------------------------------------------------------------

    @GetMapping({"/admin/users/{uuid}/sessions", "/users/{uuid}/sessions"})
    @PreAuthorize("hasAuthority('session:read')")
    public List<AdminSession> sessions(@PathVariable("uuid") UUID uuid) {
        return service().userSessions(uuid);
    }

    @DeleteMapping({"/admin/users/{uuid}/sessions/{sessionId}", "/users/{uuid}/sessions/{sessionId}"})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('session:revoke')")
    public void revokeSession(@PathVariable("uuid") UUID uuid, @PathVariable("sessionId") UUID sessionId,
                              Authentication auth) {
        service().revokeUserSession(uuid, sessionId, auth.getName());
    }

    @DeleteMapping({"/admin/users/{uuid}/sessions", "/users/{uuid}/sessions"})
    @PreAuthorize("hasAuthority('session:revoke')")
    public Map<String, Integer> revokeSessions(@PathVariable("uuid") UUID uuid, Authentication auth) {
        return Map.of("revoked", service().revokeAllUserSessions(uuid, auth.getName()));
    }

    // ---- Login monitoring -------------------------------------------------------------------------------------

    @GetMapping("/admin/login-history")
    @PreAuthorize("hasAuthority('session:read')")
    public PageResult<AdminLoginAttempt> loginHistory(@RequestParam(name = "user", required = false) UUID user,
                                                      @RequestParam(name = "search", required = false) String search,
                                                      @RequestParam(name = "status", defaultValue = "all") String status,
                                                      @RequestParam(name = "method", required = false) String method,
                                                      @RequestParam(name = "ip", required = false) String ip,
                                                      @RequestParam(name = "from", required = false) Instant from,
                                                      @RequestParam(name = "to", required = false) Instant to,
                                                      @RequestParam(name = "page", defaultValue = "0") int page,
                                                      @RequestParam(name = "size", defaultValue = "20") int size) {
        return service().loginHistory(new LoginHistoryQuery(user, search, status, method, ip, from, to, page, size));
    }

    @GetMapping("/admin/login-history/suspicious")
    @PreAuthorize("hasAuthority('session:read')")
    public List<SuspiciousActivity> suspicious(@RequestParam(name = "days", defaultValue = "7") int days) {
        return service().suspiciousActivity(days);
    }

    // ---- Audit logs -------------------------------------------------------------------------------------------

    @GetMapping("/admin/audit-logs")
    @PreAuthorize("hasAuthority('audit:read')")
    public PageResult<AuditLogEntry> auditLogs(@RequestParam(name = "category", required = false) String category,
                                               @RequestParam(name = "action", required = false) String action,
                                               @RequestParam(name = "outcome", required = false) String outcome,
                                               @RequestParam(name = "actor", required = false) String actor,
                                               @RequestParam(name = "target", required = false) String target,
                                               @RequestParam(name = "search", required = false) String search,
                                               @RequestParam(name = "from", required = false) Instant from,
                                               @RequestParam(name = "to", required = false) Instant to,
                                               @RequestParam(name = "page", defaultValue = "0") int page,
                                               @RequestParam(name = "size", defaultValue = "25") int size) {
        return service().auditLogs(new AuditQuery(category, action, outcome, actor, target, search, from, to, page, size));
    }

    @GetMapping("/admin/audit-logs/{uuid}")
    @PreAuthorize("hasAuthority('audit:read')")
    public AuditLogEntry auditLog(@PathVariable("uuid") UUID uuid) {
        return service().auditLog(uuid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Audit log entry was not found."));
    }

    private AdminConsoleUseCase service() {
        AdminConsoleUseCase service = console.getIfAvailable();
        if (service == null) throw new IllegalStateException("AdminConsoleUseCase is not wired.");
        return service;
    }
}
