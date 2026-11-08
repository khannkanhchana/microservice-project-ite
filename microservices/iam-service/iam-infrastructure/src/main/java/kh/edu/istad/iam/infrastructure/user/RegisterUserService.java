package kh.edu.istad.iam.infrastructure.user;

import kh.edu.istad.iam.application.command.RegisterUserCommand;
import kh.edu.istad.iam.application.port.out.PasswordHasher;
import kh.edu.istad.iam.application.port.out.UserRepository;
import kh.edu.istad.iam.application.result.UserResult;
import kh.edu.istad.iam.application.usecase.RegisterUserUseCase;
import kh.edu.istad.iam.domain.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class RegisterUserService implements RegisterUserUseCase {
    private final UserRepository users;
    private final PasswordHasher passwordHasher;

    public RegisterUserService(UserRepository users, PasswordHasher passwordHasher) {
        this.users = users;
        this.passwordHasher = passwordHasher;
    }

    @Override
    @Transactional
    public UserResult execute(RegisterUserCommand command) {
        if (users.existsByUsername(command.username())) {
            throw new IllegalArgumentException("Username is already in use.");
        }
        if (users.existsByEmail(command.email())) {
            throw new IllegalArgumentException("Email is already in use.");
        }

        Instant now = Instant.now();
        User user = User.builder()
                .username(command.username())
                .passwordHash(passwordHasher.hash(command.password()))
                .givenName(command.givenName())
                .familyName(command.familyName())
                .middleName(command.middleName())
                .nickname(command.nickname())
                .gender(command.gender())
                .birthdate(command.birthdate())
                .locale(command.locale())
                .profileUrl(command.profileUrl())
                .pictureUrl(command.pictureUrl())
                .websiteUrl(command.websiteUrl())
                .email(command.email())
                .phoneNumber(command.phoneNumber())
                .zoneinfo(command.zoneinfo())
                .addressStreetAddress(command.addressStreetAddress())
                .addressLocality(command.addressLocality())
                .addressRegion(command.addressRegion())
                .addressPostalCode(command.addressPostalCode())
                .addressCountry(command.addressCountry())
                .createdAt(now)
                .createdBy("self-registration")
                .passwordChangedAt(now)
                .build();

        User saved = users.save(user);
        return new UserResult(saved.getUuid(), saved.getUsername(), saved.getGivenName(), saved.getFamilyName(),
                saved.getMiddleName(), saved.getNickname(), saved.getGender(), saved.getBirthdate(), saved.getLocale(),
                saved.getProfileUrl(), saved.getPictureUrl(), saved.getWebsiteUrl(), saved.getEmail(),
                saved.isEmailVerified(), saved.getPhoneNumber(), saved.isPhoneNumberVerified(), saved.getZoneinfo(),
                saved.getAddressStreetAddress(), saved.getAddressLocality(), saved.getAddressRegion(),
                saved.getAddressPostalCode(), saved.getAddressCountry(), saved.isEnabled(), saved.isAccountNonExpired(),
                saved.isAccountNonLocked(), saved.isCredentialsNonExpired(), saved.getFailedLoginAttempts(),
                saved.getLastLoginAt(), saved.getPasswordChangedAt(), saved.getCreatedAt(), saved.getCreatedBy(),
                saved.getUpdatedAt(), saved.getUpdatedBy(), saved.isDeleted(), saved.getDeletedAt(), saved.getDeletedBy(),
                saved.getVersion());
    }
}
