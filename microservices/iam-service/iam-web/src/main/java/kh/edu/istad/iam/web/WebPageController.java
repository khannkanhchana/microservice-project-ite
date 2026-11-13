package kh.edu.istad.iam.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import kh.edu.istad.iam.application.command.ChangePasswordCommand;
import kh.edu.istad.iam.application.command.RegisterUserCommand;
import kh.edu.istad.iam.application.command.UpdateUserProfileCommand;
import kh.edu.istad.iam.application.result.*;
import kh.edu.istad.iam.application.usecase.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Instant;
import java.util.*;

@Controller
public class WebPageController {
    private static final List<String> MESSAGE_KEYS = List.of(
            "common.logout", "common.home", "common.profile", "common.editProfile", "common.password",
            "common.linkedAccounts", "common.connectedApps", "common.save", "common.cancel", "common.back",
            "common.language", "common.english", "common.khmer", "common.error", "common.success",
            "common.serviceUnavailable", "login.title", "login.username", "login.password", "login.submit",
            "login.continueWith", "login.register", "login.invalid", "login.registered",
            "login.google", "login.github", "login.facebook", "login.telegram", "register.title",
            "register.username", "register.email", "register.password", "register.confirmPassword",
            "register.givenName", "register.familyName", "register.locale", "register.submit", "register.login",
            "validation.required", "validation.email", "validation.passwordLength", "validation.passwordMismatch",
            "profile.title", "profile.email", "profile.emailVerified", "profile.locale", "profile.enabled",
            "profile.roles", "profile.permissions", "profile.editTitle", "profile.givenName", "profile.familyName",
            "profile.nickname", "profile.phoneNumber", "profile.zoneinfo", "profile.middleName", "profile.gender",
            "profile.birthdate", "profile.picture", "profile.website", "profile.address", "profile.streetAddress",
            "profile.locality", "profile.region", "profile.postalCode", "profile.country",
            "password.title", "password.current", "password.new", "password.confirm", "password.submit",
            "accounts.title", "accounts.provider", "accounts.email", "accounts.displayName", "accounts.linkedAt",
            "accounts.link", "accounts.unlink", "accounts.empty", "apps.title", "apps.name", "apps.scopes",
            "apps.revoke", "apps.empty", "consent.title", "consent.client", "consent.scopes",
            "consent.allow", "consent.cancel", "consent.description"
    );

    private final ObjectProvider<RegisterUserUseCase> register;
    private final ObjectProvider<GetUserUseCase> getUser;
    private final ObjectProvider<UpdateUserProfileUseCase> updateProfile;
    private final ObjectProvider<ChangePasswordUseCase> changePassword;
    private final ObjectProvider<ListUserIdentitiesUseCase> listIdentities;
    private final ObjectProvider<UnlinkIdentityUseCase> unlinkIdentity;
    private final ObjectProvider<LinkIdentityUseCase> linkIdentity;
    private final ObjectProvider<GetUserAuthoritiesUseCase> authorities;
    private final ObjectProvider<ListConnectedApplicationsUseCase> listApps;
    private final ObjectProvider<RevokeConnectedApplicationUseCase> revokeApp;
    private final MessageSource messages;

    public WebPageController(ObjectProvider<RegisterUserUseCase> register,
                             ObjectProvider<GetUserUseCase> getUser,
                             ObjectProvider<UpdateUserProfileUseCase> updateProfile,
                             ObjectProvider<ChangePasswordUseCase> changePassword,
                             ObjectProvider<ListUserIdentitiesUseCase> listIdentities,
                             ObjectProvider<UnlinkIdentityUseCase> unlinkIdentity,
                             ObjectProvider<LinkIdentityUseCase> linkIdentity,
                             ObjectProvider<GetUserAuthoritiesUseCase> authorities,
                             ObjectProvider<ListConnectedApplicationsUseCase> listApps,
                             ObjectProvider<RevokeConnectedApplicationUseCase> revokeApp,
                             MessageSource messages) {
        this.register = register; this.getUser = getUser; this.updateProfile = updateProfile;
        this.changePassword = changePassword; this.listIdentities = listIdentities;
        this.unlinkIdentity = unlinkIdentity; this.linkIdentity = linkIdentity; this.authorities = authorities;
        this.listApps = listApps; this.revokeApp = revokeApp; this.messages = messages;
    }

    @GetMapping("/login")
    public String login(@RequestParam(name = "error", required = false) String error,
                        @RequestParam(name = "registered", required = false) String registered,
                        Model model, HttpServletRequest request) {
        prepare(model, request);
        model.addAttribute("hasError", error != null);
        model.addAttribute("errorMessage", error == null ? null : socialLoginError(request));
        model.addAttribute("registered", registered != null);
        model.addAttribute("socialProviders", List.of("google", "github", "facebook", "telegram"));
        return "login";
    }

    @GetMapping("/register")
    public String registerPage(Model model, HttpServletRequest request) {
        prepare(model, request);
        model.addAttribute("form", new WebForms.Registration());
        model.addAttribute("errors", List.of());
        return "register";
    }

    @PostMapping("/register")
    public String register(@ModelAttribute("form") WebForms.Registration form, Model model,
                           HttpServletRequest request) {
        prepare(model, request);
        List<String> errors = validateRegistration(form);
        if (errors.isEmpty()) {
            RegisterUserUseCase useCase = register.getIfAvailable();
            if (useCase == null) errors.add(msg("common.serviceUnavailable"));
            else {
                try {
                    useCase.execute(new RegisterUserCommand(form.getUsername(), form.getPassword(), form.getGivenName(),
                            form.getFamilyName(), form.getLocale(), form.getEmail(), null, null, null, null,
                            null, null, null, null, null, null, null, null, null, null));
                    return "redirect:/login?registered";
                } catch (RuntimeException exception) {
                    errors.add(msg("common.error"));
                }
            }
        }
        model.addAttribute("form", form);
        model.addAttribute("errors", errors);
        return "register";
    }

    @GetMapping("/")
    public String index(Authentication authentication, Model model, HttpServletRequest request) {
        prepare(model, request);
        UUID uuid = userUuid(authentication);
        if (uuid == null) {
            model.addAttribute("pageError", msg("common.serviceUnavailable"));
            return "index";
        }
        GetUserUseCase get = getUser.getIfAvailable();
        GetUserAuthoritiesUseCase getAuthorities = authorities.getIfAvailable();
        if (get == null || getAuthorities == null) {
            model.addAttribute("pageError", msg("common.serviceUnavailable"));
            return "index";
        }
        try {
            model.addAttribute("user", get.execute(uuid));
            model.addAttribute("authorities", getAuthorities.execute(uuid));
        } catch (RuntimeException exception) {
            model.addAttribute("pageError", msg("common.error"));
        }
        return "index";
    }

    @GetMapping("/profile/edit")
    public String editProfile(Authentication authentication, Model model, HttpServletRequest request) {
        prepare(model, request);
        UUID uuid = userUuid(authentication);
        GetUserUseCase useCase = getUser.getIfAvailable();
        if (uuid != null && useCase != null) {
            try { model.addAttribute("form", profileForm(useCase.execute(uuid))); }
            catch (RuntimeException exception) { model.addAttribute("pageError", msg("common.error")); }
        } else model.addAttribute("pageError", msg("common.serviceUnavailable"));
        if (!model.containsAttribute("form")) model.addAttribute("form", new WebForms.Profile());
        model.addAttribute("errors", List.of());
        return "profile-edit";
    }

    @PostMapping("/profile/edit")
    public String updateProfile(@ModelAttribute("form") WebForms.Profile form, Authentication authentication,
                               Model model, HttpServletRequest request) {
        prepare(model, request);
        UUID uuid = userUuid(authentication);
        UpdateUserProfileUseCase useCase = updateProfile.getIfAvailable();
        List<String> errors = new ArrayList<>();
        if (uuid == null || useCase == null) errors.add(msg("common.serviceUnavailable"));
        else try {
            useCase.execute(new UpdateUserProfileCommand(uuid, form.getGivenName(), form.getFamilyName(),
                    form.getMiddleName(), form.getNickname(), form.getGender(), form.getBirthdate(), form.getLocale(),
                    form.getProfileUrl(), form.getPictureUrl(), form.getWebsiteUrl(), form.getPhoneNumber(),
                    form.getZoneinfo(), form.getAddressStreetAddress(), form.getAddressLocality(),
                    form.getAddressRegion(), form.getAddressPostalCode(), form.getAddressCountry(), actor(authentication)));
            return "redirect:/";
        } catch (RuntimeException exception) { errors.add(msg("common.error")); }
        model.addAttribute("form", form);
        model.addAttribute("errors", errors);
        return "profile-edit";
    }

    @GetMapping("/profile/password")
    public String passwordPage(Model model, HttpServletRequest request) {
        prepare(model, request); model.addAttribute("form", new WebForms.Password());
        model.addAttribute("errors", List.of()); return "change-password";
    }

    @PostMapping("/profile/password")
    public String changePassword(@ModelAttribute("form") WebForms.Password form, Authentication authentication,
                                 Model model, HttpServletRequest request) {
        prepare(model, request);
        UUID uuid = userUuid(authentication);
        List<String> errors = validatePassword(form);
        ChangePasswordUseCase useCase = changePassword.getIfAvailable();
        if (uuid == null || useCase == null) errors.add(msg("common.serviceUnavailable"));
        if (errors.isEmpty()) try {
            useCase.execute(new ChangePasswordCommand(uuid, form.getCurrentPassword(), form.getNewPassword(), actor(authentication)));
            model.addAttribute("success", msg("common.success"));
            model.addAttribute("form", new WebForms.Password());
        } catch (RuntimeException exception) { errors.add(msg("common.error")); model.addAttribute("form", form); }
        else model.addAttribute("form", form);
        model.addAttribute("errors", errors);
        return "change-password";
    }

    @GetMapping("/profile/accounts")
    public String linkedAccounts(Authentication authentication, Model model, HttpServletRequest request) {
        prepare(model, request); model.addAttribute("providers", List.of("google", "github", "facebook", "telegram"));
        UUID uuid = userUuid(authentication);
        ListUserIdentitiesUseCase useCase = listIdentities.getIfAvailable();
        try { model.addAttribute("identities", uuid == null || useCase == null ? List.of() : useCase.execute(uuid)); }
        catch (RuntimeException exception) { model.addAttribute("pageError", msg("common.error")); model.addAttribute("identities", List.of()); }
        return "linked-accounts";
    }

    @GetMapping("/profile/accounts/link/{provider}")
    public String linkAccount(@PathVariable String provider, Authentication authentication, HttpSession session) {
        if (!List.of("google", "github", "facebook", "telegram").contains(provider)) return "redirect:/profile/accounts";
        UUID uuid = userUuid(authentication);
        if (uuid == null || linkIdentity.getIfAvailable() == null) return "redirect:/profile/accounts?error";
        session.setAttribute("iam.link.userUuid", uuid.toString());
        return "redirect:/oauth2/authorization/" + provider;
    }

    @PostMapping("/profile/accounts/{identityUuid}/unlink")
    public String unlinkAccount(@PathVariable UUID identityUuid, Authentication authentication,
                                RedirectAttributes redirect) {
        UUID uuid = userUuid(authentication);
        UnlinkIdentityUseCase useCase = unlinkIdentity.getIfAvailable();
        if (uuid == null || useCase == null) redirect.addFlashAttribute("accountError", true);
        else try { useCase.execute(uuid, identityUuid, Instant.now(), actor(authentication)); }
        catch (RuntimeException exception) { redirect.addFlashAttribute("accountError", true); }
        return "redirect:/profile/accounts";
    }

    @GetMapping("/profile/apps")
    public String connectedApps(Authentication authentication, Model model, HttpServletRequest request) {
        prepare(model, request);
        UUID uuid = userUuid(authentication);
        ListConnectedApplicationsUseCase useCase = listApps.getIfAvailable();
        try { model.addAttribute("apps", uuid == null || useCase == null ? List.of() : useCase.execute(uuid)); }
        catch (RuntimeException exception) { model.addAttribute("pageError", msg("common.error")); model.addAttribute("apps", List.of()); }
        return "connected-apps";
    }

    @PostMapping("/profile/apps/{clientId}/revoke")
    public String revokeApp(@PathVariable String clientId, Authentication authentication) {
        UUID uuid = userUuid(authentication);
        RevokeConnectedApplicationUseCase useCase = revokeApp.getIfAvailable();
        if (uuid != null && useCase != null) useCase.execute(uuid, clientId);
        return "redirect:/profile/apps";
    }

    @GetMapping("/oauth2/consent")
    public String consent(@RequestParam(name = "client_id") String clientId,
                          @RequestParam(name = "scope", required = false) String scope,
                          @RequestParam(name = "state", required = false) String state,
                          Model model, HttpServletRequest request) {
        prepare(model, request);
        model.addAttribute("clientId", clientId);
        model.addAttribute("state", state == null ? "" : state);
        model.addAttribute("scopes", scope == null ? List.of() : Arrays.asList(scope.split(" ")));
        return "consent";
    }

    /** Social sign-in failures carry a user-facing reason; password failures keep the generic message. */
    private static String socialLoginError(HttpServletRequest request) {
        var session = request.getSession(false);
        if (session == null) return null;
        Object exception = session.getAttribute(org.springframework.security.web.WebAttributes.AUTHENTICATION_EXCEPTION);
        // OAuth2AuthenticationException lives in spring-security-oauth2-core, which this module does not depend on.
        if (exception instanceof org.springframework.security.core.AuthenticationException auth
                && auth.getClass().getName().startsWith("org.springframework.security.oauth2.")) {
            String description = auth.getMessage();
            return description == null || description.isBlank() ? "Social sign-in failed. Please try again." : description;
        }
        return null;
    }

    private void prepare(Model model, HttpServletRequest request) {
        Locale locale = LocaleContextHolder.getLocale();
        Map<String, String> localized = new LinkedHashMap<>();
        for (String key : MESSAGE_KEYS) localized.put(key, messages.getMessage(key, null, key, locale));
        model.addAttribute("messages", localized);
        model.addAttribute("lang", locale.getLanguage());
        Object csrf = request.getAttribute(CsrfToken.class.getName());
        if (csrf == null) csrf = request.getAttribute("_csrf");
        model.addAttribute("csrf", csrf);
    }

    private List<String> validateRegistration(WebForms.Registration form) {
        List<String> errors = new ArrayList<>();
        if (blank(form.getUsername()) || blank(form.getEmail()) || blank(form.getPassword())
                || blank(form.getGivenName()) || blank(form.getFamilyName()) || blank(form.getLocale())) {
            errors.add(msg("validation.required"));
        }
        if (form.getEmail() != null && !form.getEmail().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) errors.add(msg("validation.email"));
        if (form.getPassword() != null && form.getPassword().length() < 8) errors.add(msg("validation.passwordLength"));
        if (!Objects.equals(form.getPassword(), form.getConfirmPassword())) errors.add(msg("validation.passwordMismatch"));
        return errors;
    }

    private List<String> validatePassword(WebForms.Password form) {
        List<String> errors = new ArrayList<>();
        if (blank(form.getCurrentPassword()) || blank(form.getNewPassword()) || blank(form.getConfirmPassword())) errors.add(msg("validation.required"));
        if (form.getNewPassword() != null && form.getNewPassword().length() < 8) errors.add(msg("validation.passwordLength"));
        if (!Objects.equals(form.getNewPassword(), form.getConfirmPassword())) errors.add(msg("validation.passwordMismatch"));
        return errors;
    }

    private String msg(String key) { return messages.getMessage(key, null, key, LocaleContextHolder.getLocale()); }
    private static boolean blank(String value) { return value == null || value.isBlank(); }
    private static String actor(Authentication auth) { return auth == null ? "anonymous" : auth.getName(); }

    private static UUID userUuid(Authentication authentication) {
        if (authentication == null) return null;
        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthenticatedUserIdentity identity) return identity.getUserUuid();
        if (principal instanceof UserDetails details && details.getUsername() != null) {
            try { return UUID.fromString(details.getUsername()); } catch (IllegalArgumentException ignored) { }
        }
        try { return UUID.fromString(authentication.getName()); } catch (IllegalArgumentException ignored) { return null; }
    }

    private static WebForms.Profile profileForm(UserResult user) {
        WebForms.Profile form = new WebForms.Profile();
        form.setGivenName(user.givenName()); form.setFamilyName(user.familyName()); form.setMiddleName(user.middleName());
        form.setNickname(user.nickname()); form.setGender(user.gender()); form.setBirthdate(user.birthdate());
        form.setLocale(user.locale()); form.setProfileUrl(user.profileUrl()); form.setPictureUrl(user.pictureUrl());
        form.setWebsiteUrl(user.websiteUrl()); form.setPhoneNumber(user.phoneNumber()); form.setZoneinfo(user.zoneinfo());
        form.setAddressStreetAddress(user.addressStreetAddress()); form.setAddressLocality(user.addressLocality());
        form.setAddressRegion(user.addressRegion()); form.setAddressPostalCode(user.addressPostalCode());
        form.setAddressCountry(user.addressCountry());
        return form;
    }
}
