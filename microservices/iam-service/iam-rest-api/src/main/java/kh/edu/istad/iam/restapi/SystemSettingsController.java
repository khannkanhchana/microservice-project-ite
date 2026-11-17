package kh.edu.istad.iam.restapi;

import kh.edu.istad.iam.application.admin.AdminModels.AuthenticationPolicy;
import kh.edu.istad.iam.application.admin.AdminModels.PasswordPolicy;
import kh.edu.istad.iam.application.admin.AdminModels.SessionPolicy;
import kh.edu.istad.iam.application.admin.AdminModels.SystemSettings;
import kh.edu.istad.iam.application.admin.SystemSettingsUseCase;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * System configuration: authentication, password, session and token policies.
 *
 * <ul>
 *   <li>{@code GET/PUT/PATCH /api/admin/settings} — every setting (PUT replaces, PATCH changes only what is sent);</li>
 *   <li>{@code GET/PATCH /api/admin/settings/security} — the security policies only: authentication (sign-up,
 *       lockout, social providers), password and session.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/admin/settings")
public class SystemSettingsController {
    /** The sections of {@link SystemSettings} that make up the security policies. */
    private static final Set<String> SECURITY_SECTIONS = Set.of("authentication", "password", "session");

    private final ObjectProvider<SystemSettingsUseCase> settings;
    private final JsonMapper json;

    public SystemSettingsController(ObjectProvider<SystemSettingsUseCase> settings, JsonMapper json) {
        this.settings = settings;
        this.json = json;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('settings:read')")
    public SystemSettings get() {
        return service().get();
    }

    @PutMapping
    @PreAuthorize("hasAuthority('settings:update')")
    public SystemSettings update(@RequestBody SystemSettings body, Authentication auth) {
        return service().update(body, auth.getName());
    }

    /** Partial update: e.g. {@code {"tokens": {"accessTokenMinutes": 10}}} changes that one value. */
    @PatchMapping
    @PreAuthorize("hasAuthority('settings:update')")
    public SystemSettings patch(@RequestBody JsonNode body, Authentication auth) {
        return service().update(merge(service().get(), body, null), auth.getName());
    }

    @GetMapping("/security")
    @PreAuthorize("hasAuthority('settings:read')")
    public SecurityPolicies security() {
        return SecurityPolicies.of(service().get());
    }

    /** Partial update of the security policies; token lifetimes are not part of them (use PATCH /settings). */
    @PatchMapping("/security")
    @PreAuthorize("hasAuthority('settings:update')")
    public SecurityPolicies patchSecurity(@RequestBody JsonNode body, Authentication auth) {
        return SecurityPolicies.of(service().update(merge(service().get(), body, SECURITY_SECTIONS), auth.getName()));
    }

    /** Lets the UI check a password against the current policy before submitting it. */
    @PostMapping("/password-check")
    public Map<String, List<String>> checkPassword(@RequestBody Map<String, String> body) {
        return Map.of("violations", service().passwordViolations(body.get("password")));
    }

    /** Applies the sections of {@code patch} (limited to {@code allowed} when given) onto the current settings. */
    private SystemSettings merge(SystemSettings current, JsonNode patch, Set<String> allowed) {
        if (patch == null || !patch.isObject()) throw new IllegalArgumentException("Send a JSON object of settings to change.");
        ObjectNode target = json.valueToTree(current);
        for (String section : patch.propertyNames()) {
            boolean known = target.get(section) != null && target.get(section).isObject();
            if (!known || (allowed != null && !allowed.contains(section))) {
                throw new IllegalArgumentException("Unknown or read-only settings section: " + section);
            }
            JsonNode values = patch.get(section);
            if (!values.isObject()) throw new IllegalArgumentException("Settings section '" + section + "' must be an object.");
            ObjectNode existing = (ObjectNode) target.get(section);
            for (String field : values.propertyNames()) {
                if (!existing.has(field)) {
                    throw new IllegalArgumentException("Unknown setting: " + section + "." + field);
                }
                existing.set(field, values.get(field));
            }
        }
        return json.treeToValue(target, SystemSettings.class);
    }

    private SystemSettingsUseCase service() {
        SystemSettingsUseCase service = settings.getIfAvailable();
        if (service == null) throw new IllegalStateException("SystemSettingsUseCase is not wired.");
        return service;
    }

    /** Security policies view of {@link SystemSettings}. */
    public record SecurityPolicies(AuthenticationPolicy authentication, PasswordPolicy password, SessionPolicy session,
                                   Instant updatedAt, String updatedBy) {
        static SecurityPolicies of(SystemSettings s) {
            return new SecurityPolicies(s.authentication(), s.password(), s.session(), s.updatedAt(), s.updatedBy());
        }
    }
}
