package kh.edu.istad.iam.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import kh.edu.istad.iam.application.account.LoginHandoffStore;
import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Starts a social login on the same origin as the provider's registered callback, and brings the result back.
 *
 * <p>The OAuth2 authorization request is kept in the HTTP session, and session cookies are per host.
 * Telegram's callback is registered on the public ngrok URL (Telegram requires HTTPS), while users browse
 * {@code http://localhost:9000}. So a login started on localhost:
 * <ol>
 *   <li>{@code /login/social/telegram} on localhost issues an outbound {@link LoginHandoffStore} ticket
 *       (return origin + any account-link in progress) and redirects to the public host,</li>
 *   <li>{@code /login/social/telegram/continue} on the public host restores that state into its session and
 *       starts the provider login,</li>
 *   <li>after the provider callback, {@code AccountLoginHandlers} hands the signed-in user back with an inbound
 *       ticket to {@code /login/social/complete} on localhost, which signs this browser in there and resumes
 *       the original request (e.g. the iam-web {@code /oauth2/authorize}).</li>
 * </ol>
 */
@Controller
public class SocialLoginRedirectController {
    private static final List<String> PROVIDERS = List.of("google", "github", "facebook", "telegram");
    /** Session attributes of the account-link flow (AccountLoginHandlers.LINK_*), carried across hosts. */
    private static final List<String> LINK_ATTRIBUTES = List.of("iam.link.userUuid", "iam.link.returnUrl");

    private final Environment environment;
    private final LoginHandoffStore handoffs;
    private final HttpSessionSecurityContextRepository contextRepository = new HttpSessionSecurityContextRepository();
    private final HttpSessionRequestCache requestCache = new HttpSessionRequestCache();

    public SocialLoginRedirectController(Environment environment, LoginHandoffStore handoffs) {
        this.environment = environment;
        this.handoffs = handoffs;
    }

    @GetMapping("/login/social/{provider}")
    public String start(@PathVariable("provider") String provider, HttpServletRequest request) {
        if (!PROVIDERS.contains(provider) || !providerEnabled(provider)) return "redirect:/login";
        String authorizationPath = "/oauth2/authorization/" + provider;

        String callbackOrigin = origin(environment.getProperty(
                "spring.security.oauth2.client.registration." + provider + ".redirect-uri"));
        String currentOrigin = currentOrigin();
        if (callbackOrigin == null || callbackOrigin.equalsIgnoreCase(currentOrigin)) {
            return "redirect:" + authorizationPath;
        }

        // Different host: carry the return origin and any link-in-progress over with a one-time ticket.
        Map<String, Object> carried = new HashMap<>();
        HttpSession session = request.getSession(false);
        if (session != null) {
            for (String name : LINK_ATTRIBUTES) {
                Object value = session.getAttribute(name);
                if (value != null) carried.put(name, value);
                session.removeAttribute(name);
            }
        }
        String ticket = handoffs.issue(Map.of(
                LoginHandoffStore.RETURN_ORIGIN, currentOrigin,
                LoginHandoffStore.SESSION_ATTRIBUTES, carried));
        return "redirect:" + callbackOrigin + "/login/social/" + provider + "/continue?ticket=" + encode(ticket);
    }

    /** On the callback host: restore the carried state into this host's session, then start provider login. */
    @GetMapping("/login/social/{provider}/continue")
    public String continueOnCallbackHost(@PathVariable("provider") String provider,
                                         @RequestParam("ticket") String ticket, HttpServletRequest request) {
        if (!PROVIDERS.contains(provider)) return "redirect:/login";
        Map<String, Object> payload = handoffs.redeem(ticket).orElse(null);
        if (payload == null) return "redirect:/login?error";

        HttpSession session = request.getSession(true);
        copySessionAttributes(payload, session);
        Object returnOrigin = payload.get(LoginHandoffStore.RETURN_ORIGIN);
        if (returnOrigin != null) session.setAttribute(LoginHandoffStore.RETURN_ORIGIN_SESSION_ATTRIBUTE, returnOrigin);
        return "redirect:/oauth2/authorization/" + provider;
    }

    /** Back on the original host: sign this browser in with the login finished on the callback host. */
    @GetMapping("/login/social/complete")
    public String complete(@RequestParam("ticket") String ticket,
                           HttpServletRequest request, HttpServletResponse response) {
        Map<String, Object> payload = handoffs.redeem(ticket).orElse(null);
        if (payload == null) return "redirect:/login?error";

        HttpSession session = request.getSession(true);
        copySessionAttributes(payload, session);
        if (!(payload.get(LoginHandoffStore.AUTHENTICATION) instanceof Authentication authentication)) {
            // Failed provider login: the carried AUTHENTICATION_EXCEPTION is shown by the login page.
            return "redirect:/login?error";
        }

        request.changeSessionId(); // session fixation protection, as after any login
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        contextRepository.saveContext(context, request, response);

        SavedRequest saved = requestCache.getRequest(request, response);
        if (saved != null) {
            requestCache.removeRequest(request, response);
            return "redirect:" + saved.getRedirectUrl();
        }
        return "redirect:/account";
    }

    /** Social providers can be switched off by the authentication policy (Admin > Settings). */
    @org.springframework.beans.factory.annotation.Autowired
    private org.springframework.beans.factory.ObjectProvider<kh.edu.istad.iam.application.admin.SystemSettingsUseCase> policies;

    private boolean providerEnabled(String provider) {
        var settings = policies.getIfAvailable();
        return settings == null || settings.get().authentication().enabledSocialProviders().contains(provider);
    }

    @SuppressWarnings("unchecked")
    private static void copySessionAttributes(Map<String, Object> payload, HttpSession session) {
        Object attributes = payload.get(LoginHandoffStore.SESSION_ATTRIBUTES);
        if (attributes instanceof Map<?, ?> map) {
            ((Map<String, Object>) map).forEach(session::setAttribute);
        }
    }

    private static String currentOrigin() {
        return ServletUriComponentsBuilder.fromCurrentContextPath().replacePath(null).build().toUriString();
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /** Origin of an absolute redirect-uri; null for templated values such as {baseUrl}/login/oauth2/code/... */
    private static String origin(String redirectUri) {
        if (redirectUri == null || redirectUri.isBlank() || redirectUri.contains("{")) return null;
        try {
            URI uri = URI.create(redirectUri.trim());
            if (uri.getScheme() == null || uri.getHost() == null) return null;
            return uri.getScheme() + "://" + uri.getHost() + (uri.getPort() == -1 ? "" : ":" + uri.getPort());
        } catch (IllegalArgumentException invalid) {
            return null;
        }
    }
}
