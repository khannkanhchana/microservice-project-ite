package kh.edu.istad.iam.infrastructure.account;

import kh.edu.istad.iam.application.account.AccountActivityType;
import kh.edu.istad.iam.application.command.ChangePasswordCommand;
import kh.edu.istad.iam.application.command.ResetUserPasswordCommand;
import kh.edu.istad.iam.application.port.out.PasswordHasher;
import kh.edu.istad.iam.application.port.out.UserRepository;
import kh.edu.istad.iam.application.usecase.ChangePasswordUseCase;
import kh.edu.istad.iam.application.usecase.ResetUserPasswordUseCase;
import kh.edu.istad.iam.domain.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/** Self-service password change and administrator password reset. */
@Service
public class PasswordManagementService implements ChangePasswordUseCase, ResetUserPasswordUseCase {
    private final UserRepository users;
    private final PasswordHasher passwordHasher;
    private final AccountAuditLog audit;
    private final AccountSessionRegistry sessions;

    public PasswordManagementService(UserRepository users, PasswordHasher passwordHasher, AccountAuditLog audit,
                                     AccountSessionRegistry sessions) {
        this.users = users;
        this.passwordHasher = passwordHasher;
        this.audit = audit;
        this.sessions = sessions;
    }

    @Override
    @Transactional
    public void execute(ChangePasswordCommand command) {
        User user = users.findByUuid(command.userUuid())
                .orElseThrow(() -> new IllegalArgumentException("User was not found."));
        String existingHash = user.getPasswordHash();
        boolean hadPassword = existingHash != null && !existingHash.isBlank();
        if (hadPassword) {
            if (command.currentPassword() == null || !passwordHasher.matches(command.currentPassword(), existingHash)) {
                throw new IllegalArgumentException("Current password is incorrect.");
            }
            if (passwordHasher.matches(command.newPassword(), existingHash)) {
                throw new IllegalArgumentException("New password must be different from the current password.");
            }
        }
        user.changePassword(passwordHasher.hash(command.newPassword()), Instant.now(), command.changedBy());
        users.save(user);
        audit.activity(user.getUuid(), AccountActivityType.PASSWORD_CHANGED, hadPassword ? "changed" : "set",
                command.changedBy());
    }

    @Override
    @Transactional
    public void execute(ResetUserPasswordCommand command) {
        User user = users.findByUuid(command.userUuid())
                .orElseThrow(() -> new IllegalArgumentException("User was not found."));
        user.changePassword(passwordHasher.hash(command.newPassword()), Instant.now(), command.resetBy());
        users.save(user);
        audit.activity(user.getUuid(), AccountActivityType.PASSWORD_RESET, "reset by administrator", command.resetBy());
        sessions.revokeAll(user.getUuid(), null, "password_reset_by_admin");
    }
}
