package kh.edu.istad.iam.application.admin;

import kh.edu.istad.iam.application.admin.AdminModels.SystemSettings;

import java.util.List;

/** Runtime configuration: authentication, password, session and token policies. */
public interface SystemSettingsUseCase {

    SystemSettings get();

    SystemSettings update(SystemSettings settings, String actor);

    /** Human-readable violations of the password policy; empty when the password is acceptable. */
    List<String> passwordViolations(String password);

    /** Throws IllegalArgumentException listing the violations. */
    default void requireValidPassword(String password) {
        List<String> violations = passwordViolations(password);
        if (!violations.isEmpty()) throw new IllegalArgumentException(String.join(" ", violations));
    }
}
