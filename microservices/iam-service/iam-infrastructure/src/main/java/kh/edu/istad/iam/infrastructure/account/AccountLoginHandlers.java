package kh.edu.istad.iam.infrastructure.account;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import kh.edu.istad.iam.application.account.AccountActivityType;
import kh.edu.istad.iam.application.account.LoginHandoffStore;
import kh.edu.istad.iam.infrastructure.admin.LoginLockoutService;
import kh.edu.istad.iam.infrastructure.admin.SystemSettingsService;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Login success/failure handlers shared by password and social login. They write the login history,
 * start a device session for the browser, and finish the "link a social account" flow started from iam-web.
 */
@Component
public class AccountLoginHandlers {
    /** Session attribute used by FederatedUserProvisioner to link instead of sign in. */
    public static final String LINK_USER_ATTRIBUTE = "iam.link.userUuid";
    /** Where to send the browser after a link flow started from the React app. */
    public static final String LINK_RETURN_ATTRIBUTE = "iam.link.returnUrl";

    private final AccountSessionRegistry sessions;
    private final AccountAuditLog audit;
    private final ClientInfoResolver clients;
    private final LoginHandoffStore handoffs;
    private final LoginLockoutService lockout;
    private final SystemSettingsService settings;

    public AccountLoginHandlers(AccountSessionRegistry sessions, AccountAuditLog audit, ClientInfoResolver clients,
                                LoginHandoffStore handoffs, LoginLockoutService lockout,
                                SystemSettingsService settings) {
        this.sessions = sessions;
        this.audit = audit;
        this.clients = clients;
        this.handoffs = handoffs;
        this.lockout = lockout;
        this.settings = settings;
    }

    public AuthenticationSuccessHandler successHandler() {
        SavedRequestAwareAuthenticationSuccessHandler delegate = new SavedRequestAwareAuthenticationSuccessHandler();
        delegate.setDefaultTargetUrl("/account"); // the iam-web React app served by this service
        return (request, response, authentication) -> {
            String returnUrl = onSuccess(request, authentication);
            if (returnUrl == null) returnUrl = handBack(request, authentication, null);
            if (returnUrl != null) response.sendRedirect(returnUrl);
            else delegate.onAuthenticationSuccess(request, response, authentication);
        };
    }

    public AuthenticationFailureHandler failureHandler() {
        SimpleUrlAuthenticationFailureHandler delegate = new SimpleUrlAuthenticationFailureHandler("/login?error");
        return new AuthenticationFailureHandler() {
            @Override
            public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                                AuthenticationException exception) throws IOException, ServletException {
                String returnUrl = onFailure(request, exception);
                if (returnUrl == null) returnUrl = handBack(request, null, exception);
                if (returnUrl != null) response.sendRedirect(returnUrl);
                else delegate.onAuthenticationFailure(request, response, exception);
            }
        };
    }

    /**
     * When this social login ran on the public callback host (see SocialLoginRedirectController), returns the URL
     * that hands the result back to the host the user started on; null for an ordinary same-host login.
     */
    private String handBack(HttpServletRequest request, Authentication authentication, AuthenticationException failure) {
        HttpSession session = request.getSession(false);
        if (session == null) return null;
        Object returnOrigin = session.getAttribute(LoginHandoffStore.RETURN_ORIGIN_SESSION_ATTRIBUTE);
        if (!(returnOrigin instanceof String origin) || origin.isBlank()) return null;
        session.removeAttribute(LoginHandoffStore.RETURN_ORIGIN_SESSION_ATTRIBUTE);

        Map<String, Object> carried = new HashMap<>();
        Map<String, Object> payload = new HashMap<>();
        if (authentication != null) {
            Object accountSession = session.getAttribute(AccountSessionRegistry.SESSION_ATTRIBUTE);
            if (accountSession != null) carried.put(AccountSessionRegistry.SESSION_ATTRIBUTE, accountSession);
            payload.put(LoginHandoffStore.AUTHENTICATION, authentication);
            // The user continues on the original host; this host's copy of the login is no longer needed.
            session.invalidate();
        } else if (failure != null) {
            carried.put(WebAttributes.AUTHENTICATION_EXCEPTION, failure);
        }
        payload.put(LoginHandoffStore.SESSION_ATTRIBUTES, carried);
        return origin + "/login/social/complete?ticket="
                + URLEncoder.encode(handoffs.issue(payload), StandardCharsets.UTF_8);
    }

    /** Returns the React app URL to redirect to when this login completed a link flow. */
    private String onSuccess(HttpServletRequest request, Authentication authentication) {
        UUID userUuid = Principals.userUuid(authentication.getPrincipal());
        String method = method(authentication);
        ClientInfo client = clients.resolve(request);
        HttpSession session = request.getSession();
        String returnUrl = (String) session.getAttribute(LINK_RETURN_ATTRIBUTE);
        session.removeAttribute(LINK_RETURN_ATTRIBUTE);
        boolean linking = returnUrl != null && authentication instanceof OAuth2AuthenticationToken;

        if (userUuid != null) {
            if (linking) {
                audit.activity(userUuid, AccountActivityType.IDENTITY_LINKED, method, userUuid.toString(), client);
            } else {
                audit.loginAttempt(userUuid, authentication.getName(), true, method, null, client);
                lockout.recordSuccess(userUuid);
            }
            // Session expiration policy: the browser's IAM session (stored in Redis) idles out after this.
            session.setMaxInactiveInterval(settings.get().session().idleTimeoutMinutes() * 60);
            UUID existing = Principals.parse((String) session.getAttribute(AccountSessionRegistry.SESSION_ATTRIBUTE));
            if (!userUuid.equals(sessions.activeOwner(existing))) {
                UUID sessionId = sessions.start(userUuid, method, client, session.getId());
                session.setAttribute(AccountSessionRegistry.SESSION_ATTRIBUTE, sessionId.toString());
            }
        }
        return linking ? withParam(returnUrl, "linked", method) : null;
    }

    private String onFailure(HttpServletRequest request, AuthenticationException exception) {
        HttpSession session = request.getSession(false);
        String returnUrl = session == null ? null : (String) session.getAttribute(LINK_RETURN_ATTRIBUTE);
        String method = request.getRequestURI().contains("/login/oauth2/code/")
                ? request.getRequestURI().substring(request.getRequestURI().lastIndexOf('/') + 1)
                : "password";
        if (returnUrl != null && !"password".equals(method)) {
            session.removeAttribute(LINK_RETURN_ATTRIBUTE);
            session.removeAttribute(LINK_USER_ATTRIBUTE);
            return withParam(returnUrl, "linkError", message(exception));
        }
        String identifier = request.getParameter("username");
        UUID userUuid = audit.findUserUuid(identifier);
        audit.loginAttempt(userUuid, identifier, false, method, reason(exception), clients.resolve(request));
        if (exception instanceof BadCredentialsException) lockout.recordFailure(userUuid);
        return null;
    }

    private static String method(Authentication authentication) {
        return authentication instanceof OAuth2AuthenticationToken token
                ? token.getAuthorizedClientRegistrationId() : "password";
    }

    private static String reason(AuthenticationException exception) {
        if (exception instanceof BadCredentialsException) return "invalid_credentials";
        if (exception instanceof DisabledException) return "account_disabled";
        if (exception instanceof LockedException) return "account_locked";
        if (exception instanceof AccountExpiredException) return "account_expired";
        if (exception instanceof CredentialsExpiredException) return "credentials_expired";
        if (exception instanceof OAuth2AuthenticationException) return "social_login_failed: " + message(exception);
        return exception.getClass().getSimpleName();
    }

    private static String message(AuthenticationException exception) {
        if (exception instanceof OAuth2AuthenticationException oauth && oauth.getError().getDescription() != null) {
            return oauth.getError().getDescription();
        }
        return exception.getMessage() == null ? "Authentication failed" : exception.getMessage();
    }

    private static String withParam(String url, String name, String value) {
        return url + (url.contains("?") ? "&" : "?") + name + "=" + URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
