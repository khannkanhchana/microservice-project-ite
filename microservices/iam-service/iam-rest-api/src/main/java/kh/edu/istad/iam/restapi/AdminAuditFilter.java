package kh.edu.istad.iam.restapi;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kh.edu.istad.iam.application.admin.AdminConsoleUseCase;
import kh.edu.istad.iam.application.admin.AdminModels.AuditEvent;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Records every change made through the administration API (users, role and permission assignments, roles,
 * permissions, clients, sessions, account security, settings) in the audit log: who, what, which target,
 * the outcome (HTTP status) and the request body with secrets masked. Read-only requests are not recorded.
 */
@Component
public class AdminAuditFilter extends OncePerRequestFilter {
    private static final String UUID_RE = "([0-9a-fA-F-]{36})";
    private static final Pattern SECRET_FIELDS = Pattern.compile(
            "(\"(?:password|newPassword|currentPassword|confirmPassword|clientSecret|secret)\"\\s*:\\s*)\"[^\"]*\"");

    /** Route → category/action/target type. The first matching rule wins. */
    private static final List<Rule> RULES = List.of(
            rule("POST", "/api/users/" + UUID_RE + "/roles", "ROLE_ASSIGNMENT", "ASSIGN_ROLE", "USER"),
            rule("DELETE", "/api/users/" + UUID_RE + "/roles/.+", "ROLE_ASSIGNMENT", "REVOKE_ROLE", "USER"),
            rule("POST", "/api/users/" + UUID_RE + "/permissions", "PERMISSION_ASSIGNMENT", "GRANT_PERMISSION", "USER"),
            rule("DELETE", "/api/users/" + UUID_RE + "/permissions/.+", "PERMISSION_ASSIGNMENT", "REVOKE_PERMISSION", "USER"),
            rule("POST", "/api/users/" + UUID_RE + "/enable", "USER", "ACTIVATE", "USER"),
            rule("POST", "/api/users/" + UUID_RE + "/disable", "USER", "DEACTIVATE", "USER"),
            rule("POST", "/api/users/" + UUID_RE + "/lock", "SECURITY", "LOCK", "USER"),
            rule("POST", "/api/users/" + UUID_RE + "/unlock", "SECURITY", "UNLOCK", "USER"),
            rule("PUT", "/api/users/" + UUID_RE + "/password", "SECURITY", "SET_PASSWORD", "USER"),
            rule("DELETE", "/api/users/" + UUID_RE + "/identities/.+", "USER", "UNLINK_IDENTITY", "USER"),
            rule("POST", "/api/users", "USER", "CREATE", "USER"),
            rule("PATCH", "/api/users/" + UUID_RE, "USER", "UPDATE", "USER"),
            rule("DELETE", "/api/users/" + UUID_RE, "USER", "DELETE", "USER"),
            rule("POST", "/api/roles/" + UUID_RE + "/permissions", "ROLE_PERMISSION", "ASSIGN_PERMISSION", "ROLE"),
            rule("DELETE", "/api/roles/" + UUID_RE + "/permissions/.+", "ROLE_PERMISSION", "REVOKE_PERMISSION", "ROLE"),
            rule("POST", "/api/roles/" + UUID_RE + "/activate", "ROLE", "ACTIVATE", "ROLE"),
            rule("POST", "/api/roles/" + UUID_RE + "/deactivate", "ROLE", "DEACTIVATE", "ROLE"),
            rule("POST", "/api/roles", "ROLE", "CREATE", "ROLE"),
            rule("PATCH", "/api/roles/" + UUID_RE, "ROLE", "UPDATE", "ROLE"),
            rule("DELETE", "/api/roles/" + UUID_RE, "ROLE", "DELETE", "ROLE"),
            rule("POST", "/api/permissions/" + UUID_RE + "/activate", "PERMISSION", "ACTIVATE", "PERMISSION"),
            rule("POST", "/api/permissions/" + UUID_RE + "/deactivate", "PERMISSION", "DEACTIVATE", "PERMISSION"),
            rule("POST", "/api/permissions", "PERMISSION", "CREATE", "PERMISSION"),
            rule("PATCH", "/api/permissions/" + UUID_RE, "PERMISSION", "UPDATE", "PERMISSION"),
            rule("DELETE", "/api/permissions/" + UUID_RE, "PERMISSION", "DELETE", "PERMISSION"),
            rule("POST", "/api/admin/users/" + UUID_RE + "/unlock", "SECURITY", "UNLOCK", "USER"),
            rule("POST", "/api/admin/users/" + UUID_RE + "/password-reset-link", "SECURITY", "SEND_RESET_LINK", "USER"),
            rule("DELETE", "/api/admin/users/" + UUID_RE + "/sessions/.+", "SESSION", "TERMINATE", "USER"),
            rule("DELETE", "/api/admin/users/" + UUID_RE + "/sessions", "SESSION", "TERMINATE_ALL", "USER"),
            rule("POST", "/api/admin/clients/([^/]+)/secret", "CLIENT", "ROTATE_SECRET", "CLIENT"),
            rule("POST", "/api/admin/clients", "CLIENT", "CREATE", "CLIENT"),
            rule("PUT", "/api/admin/clients/([^/]+)", "CLIENT", "UPDATE", "CLIENT"),
            rule("DELETE", "/api/admin/clients/([^/]+)", "CLIENT", "DELETE", "CLIENT"),
            rule("PUT", "/api/admin/settings", "CONFIGURATION", "UPDATE_SETTINGS", "SETTINGS"));

    private final ObjectProvider<AdminConsoleUseCase> console;

    public AdminAuditFilter(ObjectProvider<AdminConsoleUseCase> console) {
        this.console = console;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String method = request.getMethod();
        return "GET".equals(method) || "HEAD".equals(method) || "OPTIONS".equals(method) || match(request) == null;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        ContentCachingRequestWrapper wrapped = new ContentCachingRequestWrapper(request, 16 * 1024);
        Matched matched = match(request);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        try {
            chain.doFilter(wrapped, response);
        } finally {
            record(wrapped, response, matched, auth);
        }
    }

    private void record(ContentCachingRequestWrapper request, HttpServletResponse response, Matched m,
                        Authentication auth) {
        AdminConsoleUseCase service = console.getIfAvailable();
        if (service == null || m == null) return;
        int status = response.getStatus();
        String body = new String(request.getContentAsByteArray(), StandardCharsets.UTF_8);
        body = SECRET_FIELDS.matcher(body).replaceAll("$1\"***\"");
        String actor = auth == null ? "anonymous" : auth.getName();
        String description = m.rule().action().replace('_', ' ').toLowerCase() + " "
                + m.rule().targetType().toLowerCase() + (m.target() == null ? "" : " " + m.target());
        service.audit(new AuditEvent(m.rule().category(), m.rule().action(), status < 400 ? "SUCCESS" : "FAILURE",
                actor, null, m.rule().targetType(), m.target(), description, body.isBlank() ? null : body,
                clientIp(request), request.getHeader("User-Agent"), request.getMethod(), request.getRequestURI(),
                status));
    }

    private static Matched match(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        for (Rule rule : RULES) {
            if (!rule.method().equals(request.getMethod())) continue;
            Matcher matcher = rule.pattern().matcher(path);
            if (matcher.matches()) return new Matched(rule, matcher.groupCount() > 0 ? matcher.group(1) : null);
        }
        return null;
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) return forwarded.split(",")[0].trim();
        return request.getRemoteAddr();
    }

    private static Rule rule(String method, String regex, String category, String action, String targetType) {
        return new Rule(method, Pattern.compile(regex), category, action, targetType);
    }

    private record Rule(String method, Pattern pattern, String category, String action, String targetType) { }

    private record Matched(Rule rule, String target) { }
}
