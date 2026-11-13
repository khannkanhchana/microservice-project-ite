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

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Administration console API: overview, user account security, user sessions, login monitoring and audit logs.
 * User/role/permission CRUD and assignments live in UserController, RoleController and PermissionController.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final ObjectProvider<AdminConsoleUseCase> console;
    private final String webBaseUrl;

    public AdminController(ObjectProvider<AdminConsoleUseCase> console,
                           @Value("${iam.web.base-url:http://localhost:9000}") String webBaseUrl) {
        this.console = console;
        this.webBaseUrl = webBaseUrl.endsWith("/") ? webBaseUrl.substring(0, webBaseUrl.length() - 1) : webBaseUrl;
    }

    @GetMapping("/overview")
    @PreAuthorize("hasAuthority('dashboard:read')")
    public Overview overview() {
        return service().overview();
    }

    // ---- Users ------------------------------------------------------------------------------------------------

    @GetMapping("/users")
    @PreAuthorize("hasAuthority('user:read')")
    public PageResult<AdminUserRow> users(@RequestParam(name = "search", required = false) String search,
                                          @RequestParam(name = "status", defaultValue = "all") String status,
                                          @RequestParam(name = "role", required = false) UUID role,
                                          @RequestParam(name = "page", defaultValue = "0") int page,
                                          @RequestParam(name = "size", defaultValue = "20") int size) {
        return service().users(new UserQuery(search, status, role, page, size));
    }

    @GetMapping("/roles/{uuid}/permissions")
    @PreAuthorize("hasAuthority('role:read')")
    public List<RolePermission> rolePermissions(@PathVariable("uuid") UUID uuid) {
        return service().rolePermissions(uuid);
    }

    @GetMapping("/users/{uuid}/security")
    @PreAuthorize("hasAuthority('user:read')")
    public AccountSecurity security(@PathVariable("uuid") UUID uuid) {
        return service().accountSecurity(uuid);
    }

    @PostMapping("/users/{uuid}/unlock")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('user:update')")
    public void unlock(@PathVariable("uuid") UUID uuid, Authentication auth) {
        service().unlockAccount(uuid, auth.getName());
    }

    @PostMapping("/users/{uuid}/password-reset-link")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasAuthority('user:update')")
    public void sendPasswordResetLink(@PathVariable("uuid") UUID uuid, Authentication auth) {
        service().sendPasswordResetLink(uuid, webBaseUrl + "/reset-password", auth.getName());
    }

    // ---- Sessions ---------------------------------------------------------------------------------------------

    @GetMapping("/users/{uuid}/sessions")
    @PreAuthorize("hasAuthority('session:read')")
    public List<AdminSession> sessions(@PathVariable("uuid") UUID uuid) {
        return service().userSessions(uuid);
    }

    @DeleteMapping("/users/{uuid}/sessions/{sessionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('session:revoke')")
    public void revokeSession(@PathVariable("uuid") UUID uuid, @PathVariable("sessionId") UUID sessionId,
                              Authentication auth) {
        service().revokeUserSession(uuid, sessionId, auth.getName());
    }

    @DeleteMapping("/users/{uuid}/sessions")
    @PreAuthorize("hasAuthority('session:revoke')")
    public Map<String, Integer> revokeSessions(@PathVariable("uuid") UUID uuid, Authentication auth) {
        return Map.of("revoked", service().revokeAllUserSessions(uuid, auth.getName()));
    }

    // ---- Login monitoring -------------------------------------------------------------------------------------

    @GetMapping("/login-history")
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

    @GetMapping("/login-history/suspicious")
    @PreAuthorize("hasAuthority('session:read')")
    public List<SuspiciousActivity> suspicious(@RequestParam(name = "days", defaultValue = "7") int days) {
        return service().suspiciousActivity(days);
    }

    // ---- Audit logs -------------------------------------------------------------------------------------------

    @GetMapping("/audit-logs")
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

    private AdminConsoleUseCase service() {
        AdminConsoleUseCase service = console.getIfAvailable();
        if (service == null) throw new IllegalStateException("AdminConsoleUseCase is not wired.");
        return service;
    }
}
