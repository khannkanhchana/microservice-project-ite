package kh.edu.istad.iam.restapi;

import kh.edu.istad.iam.application.admin.SystemSettingsUseCase;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import kh.edu.istad.iam.application.account.*;
import kh.edu.istad.iam.application.result.PageResult;
import kh.edu.istad.iam.application.usecase.GetUserUseCase;
import kh.edu.istad.iam.restapi.dto.UserRequests;
import kh.edu.istad.iam.restapi.dto.UserResponses;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Self-service account security for the iam-web React app: sessions, login history, activity, profile picture,
 * social account linking, password recovery and account deactivation.
 */
@Validated
@RestController
@RequestMapping("/api")
public class AccountController {

    /** System Configuration: registration and password policies (Admin > Settings). */
    @org.springframework.beans.factory.annotation.Autowired
    private ObjectProvider<SystemSettingsUseCase> policies;

    private void requireValidPassword(String password) {
        SystemSettingsUseCase settings = policies.getIfAvailable();
        if (settings != null) settings.requireValidPassword(password);
    }
    private static final Map<String, String> PROVIDERS = Map.of(
            "google", "Google", "github", "GitHub", "facebook", "Facebook", "telegram", "Telegram");
    private static final String CURRENT_SESSION = "iam.currentSessionId";

    private final ObjectProvider<AccountSelfServiceUseCase> account;
    private final ObjectProvider<GetUserUseCase> getUser;
    private final Environment environment;
    private final List<String> allowedOrigins;
    private final String webBaseUrl;

    public AccountController(ObjectProvider<AccountSelfServiceUseCase> account, ObjectProvider<GetUserUseCase> getUser,
                             Environment environment,
                             @Value("${iam.web.allowed-origins:http://localhost:9000,http://127.0.0.1:9000,http://localhost:5173,http://127.0.0.1:5173}") String allowedOrigins,
                             @Value("${iam.web.base-url:http://localhost:9000}") String webBaseUrl) {
        this.account = account;
        this.getUser = getUser;
        this.environment = environment;
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(",")).map(String::trim)
                .filter(origin -> !origin.isEmpty()).toList();
        this.webBaseUrl = webBaseUrl.endsWith("/") ? webBaseUrl.substring(0, webBaseUrl.length() - 1) : webBaseUrl;
    }

    // ---- Sessions -------------------------------------------------------------------------------------------

    @GetMapping("/me/sessions")
    @PreAuthorize("isAuthenticated()")
    public List<AccountSessionResult> sessions(Authentication auth,
                                               @RequestAttribute(name = CURRENT_SESSION, required = false) UUID current) {
        return service().sessions(subject(auth), current);
    }

    /** Ends one session. {@code current} signs out this device. */
    @DeleteMapping("/me/sessions/{sessionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("isAuthenticated()")
    public void revokeSession(@PathVariable String sessionId, Authentication auth,
                              @RequestAttribute(name = CURRENT_SESSION, required = false) UUID current) {
        UUID target;
        if ("current".equalsIgnoreCase(sessionId)) {
            if (current == null) return;
            target = current;
        } else {
            try { target = UUID.fromString(sessionId); }
            catch (IllegalArgumentException exception) { throw new IllegalArgumentException("Unknown session."); }
        }
        service().revokeSession(subject(auth), target, actor(auth));
    }

    /** {@code scope=others} keeps this device signed in; {@code scope=all} signs out everywhere. */
    @DeleteMapping("/me/sessions")
    @PreAuthorize("isAuthenticated()")
    public UserResponses.SessionsRevoked revokeSessions(@RequestParam(defaultValue = "others") String scope,
                                                        Authentication auth,
                                                        @RequestAttribute(name = CURRENT_SESSION, required = false) UUID current) {
        UUID keep = "all".equalsIgnoreCase(scope) ? null : current;
        return new UserResponses.SessionsRevoked(service().revokeOtherSessions(subject(auth), keep, actor(auth)));
    }

    // ---- History & activity ---------------------------------------------------------------------------------

    @GetMapping("/me/login-history")
    @PreAuthorize("isAuthenticated()")
    public PageResult<LoginAttemptResult> loginHistory(@RequestParam(defaultValue = "all") String status,
                                                       @RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "20") int size,
                                                       Authentication auth) {
        Boolean success = switch (status.toLowerCase(Locale.ROOT)) {
            case "success" -> Boolean.TRUE;
            case "failed" -> Boolean.FALSE;
            default -> null;
        };
        return service().loginHistory(subject(auth), success, page, size);
    }

    @GetMapping("/me/activity")
    @PreAuthorize("isAuthenticated()")
    public PageResult<AccountActivityResult> activity(@RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "20") int size,
                                                      Authentication auth) {
        return service().activity(subject(auth), page, size);
    }

    // ---- Deactivation ---------------------------------------------------------------------------------------

    @PostMapping("/me/deactivation")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("isAuthenticated()")
    public void deactivate(@Valid @RequestBody UserRequests.Deactivation body, Authentication auth) {
        UUID uuid = subject(auth);
        String username = required(getUser, "GetUserUseCase").execute(uuid).username();
        if (!username.equals(body.confirmation().trim())) {
            throw new IllegalArgumentException("Type your username exactly to confirm deactivation.");
        }
        service().deactivate(uuid, body.password(), body.reason(), actor(auth));
    }

    // ---- Profile picture ------------------------------------------------------------------------------------

    @PostMapping(value = "/me/picture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public UserResponses.PictureUpload uploadPicture(@RequestParam("file") MultipartFile file, Authentication auth)
            throws IOException {
        String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
        return new UserResponses.PictureUpload(service().storePicture(subject(auth), file.getContentType(),
                file.getBytes(), baseUrl, actor(auth)));
    }

    @DeleteMapping("/me/picture")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("isAuthenticated()")
    public void removePicture(Authentication auth) {
        service().removePicture(subject(auth), actor(auth));
    }

    @GetMapping("/users/{uuid}/picture")
    public ResponseEntity<byte[]> picture(@PathVariable UUID uuid) {
        return service().picture(uuid)
                .map(picture -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(picture.contentType()))
                        .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePublic())
                        .header("X-Content-Type-Options", "nosniff")
                        .body(picture.data()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // ---- Social account linking -----------------------------------------------------------------------------

    @GetMapping("/auth/providers")
    public List<UserResponses.AuthProvider> providers() {
        return PROVIDERS.entrySet().stream()
                .filter(entry -> configured(entry.getKey()))
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new UserResponses.AuthProvider(entry.getKey(), entry.getValue()))
                .toList();
    }

    /** Issues a one-time ticket; the browser then opens the returned URL to authenticate with the provider. */
    @PostMapping("/me/identities/link")
    @PreAuthorize("isAuthenticated()")
    public UserResponses.LinkStart startLink(@Valid @RequestBody UserRequests.LinkIdentity body, Authentication auth) {
        String provider = body.provider().toLowerCase(Locale.ROOT);
        if (!PROVIDERS.containsKey(provider) || !configured(provider)) {
            throw new IllegalArgumentException("This sign-in provider is not available.");
        }
        requireAllowedReturnUrl(body.returnUrl());
        String ticket = service().createLinkTicket(subject(auth), provider, body.returnUrl());
        String url = ServletUriComponentsBuilder.fromCurrentContextPath().path("/api/auth/link")
                .queryParam("ticket", ticket).build().toUriString();
        return new UserResponses.LinkStart(url);
    }

    /** Browser entry point of the link flow: binds the ticket to this browser session, then starts provider login. */
    @GetMapping("/auth/link")
    public void link(@RequestParam String ticket, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        LinkTicket link = service().consumeLinkTicket(ticket).orElse(null);
        if (link == null) {
            response.sendRedirect(webBaseUrl + "/account/linked-accounts?linkError="
                    + java.net.URLEncoder.encode("The link request expired. Please try again.",
                    java.nio.charset.StandardCharsets.UTF_8));
            return;
        }
        HttpSession session = request.getSession(true);
        session.setAttribute("iam.link.userUuid", link.userUuid().toString());
        session.setAttribute("iam.link.returnUrl", link.returnUrl());
        response.sendRedirect(request.getContextPath() + "/login/social/" + link.provider());
    }

    // ---- Password recovery ----------------------------------------------------------------------------------

    @PostMapping("/auth/password/forgot")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void forgotPassword(@Valid @RequestBody UserRequests.ForgotPassword body) {
        service().requestPasswordReset(body.email(), webBaseUrl + "/reset-password");
    }

    @PostMapping("/auth/password/reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@Valid @RequestBody UserRequests.ResetPassword body) {
        if (!body.newPassword().equals(body.confirmPassword())) {
            throw new IllegalArgumentException("confirmPassword must match newPassword.");
        }
        requireValidPassword(body.newPassword());
        service().resetPassword(body.token(), body.newPassword());
    }

    // ---- helpers --------------------------------------------------------------------------------------------

    /** Public policy hints for the sign-up and password forms. */
    @GetMapping("/auth/policy")
    public Map<String, Object> policy() {
        SystemSettingsUseCase settings = policies.getIfAvailable();
        if (settings == null) return Map.of();
        var current = settings.get();
        return Map.of("allowSelfRegistration", current.authentication().allowSelfRegistration(),
                "password", current.password());
    }

    private boolean configured(String provider) {
        SystemSettingsUseCase settings = policies.getIfAvailable();
        if (settings != null && !settings.get().authentication().enabledSocialProviders().contains(provider)) {
            return false; // disabled by the authentication policy
        }
        String clientId = environment.getProperty("spring.security.oauth2.client.registration." + provider + ".client-id");
        return clientId != null && !clientId.isBlank();
    }

    private void requireAllowedReturnUrl(String returnUrl) {
        try {
            URI uri = URI.create(returnUrl);
            String origin = uri.getScheme() + "://" + uri.getHost() + (uri.getPort() == -1 ? "" : ":" + uri.getPort());
            if (uri.getHost() != null && allowedOrigins.contains(origin)) return;
        } catch (IllegalArgumentException ignored) {
            // fall through
        }
        throw new IllegalArgumentException("returnUrl must point to the iam-web application.");
    }

    private AccountSelfServiceUseCase service() {
        AccountSelfServiceUseCase service = account.getIfAvailable();
        if (service == null) throw new UseCaseUnavailableException("AccountSelfServiceUseCase");
        return service;
    }

    private static <T> T required(ObjectProvider<T> provider, String useCase) {
        T bean = provider.getIfAvailable();
        if (bean == null) throw new UseCaseUnavailableException(useCase);
        return bean;
    }

    private static UUID subject(Authentication auth) {
        try { return UUID.fromString(auth.getName()); }
        catch (RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token subject must be the user's UUID.");
        }
    }

    private static String actor(Authentication auth) { return auth == null ? "anonymous" : auth.getName(); }
}
