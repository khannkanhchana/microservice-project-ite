package kh.edu.istad.iam.restapi;

import kh.edu.istad.iam.application.admin.AdminModels.SystemSettings;
import kh.edu.istad.iam.application.admin.SystemSettingsUseCase;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** System configuration: authentication, password, session and token policies. */
@RestController
@RequestMapping("/api/admin/settings")
public class SystemSettingsController {
    private final ObjectProvider<SystemSettingsUseCase> settings;

    public SystemSettingsController(ObjectProvider<SystemSettingsUseCase> settings) {
        this.settings = settings;
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

    /** Lets the UI check a password against the current policy before submitting it. */
    @PostMapping("/password-check")
    public Map<String, List<String>> checkPassword(@RequestBody Map<String, String> body) {
        return Map.of("violations", service().passwordViolations(body.get("password")));
    }

    private SystemSettingsUseCase service() {
        SystemSettingsUseCase service = settings.getIfAvailable();
        if (service == null) throw new IllegalStateException("SystemSettingsUseCase is not wired.");
        return service;
    }
}
