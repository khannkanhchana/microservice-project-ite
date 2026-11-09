package kh.edu.istad.iam.application.service;

import kh.edu.istad.iam.application.port.out.UserRepository;
import kh.edu.istad.iam.application.result.UserResult;
import kh.edu.istad.iam.application.usecase.GetUserUseCase;
import kh.edu.istad.iam.domain.User;

import java.util.UUID;

/** Loads a user profile through the user repository output port. */
public final class GetUserService implements GetUserUseCase {
    private final UserRepository users;

    public GetUserService(UserRepository users) {
        this.users = users;
    }

    @Override
    public UserResult execute(UUID userUuid) {
        User user = users.findByUuid(userUuid)
                .orElseThrow(() -> new IllegalArgumentException("User was not found."));
        return new UserResult(user.getUuid(), user.getUsername(), user.getGivenName(), user.getFamilyName(),
                user.getMiddleName(), user.getNickname(), user.getGender(), user.getBirthdate(), user.getLocale(),
                user.getProfileUrl(), user.getPictureUrl(), user.getWebsiteUrl(), user.getEmail(),
                user.isEmailVerified(), user.getPhoneNumber(), user.isPhoneNumberVerified(), user.getZoneinfo(),
                user.getAddressStreetAddress(), user.getAddressLocality(), user.getAddressRegion(),
                user.getAddressPostalCode(), user.getAddressCountry(), user.isEnabled(), user.isAccountNonExpired(),
                user.isAccountNonLocked(), user.isCredentialsNonExpired(), user.getFailedLoginAttempts(),
                user.getLastLoginAt(), user.getPasswordChangedAt(), user.getCreatedAt(), user.getCreatedBy(),
                user.getUpdatedAt(), user.getUpdatedBy(), user.isDeleted(), user.getDeletedAt(), user.getDeletedBy(),
                user.getVersion());
    }
}
