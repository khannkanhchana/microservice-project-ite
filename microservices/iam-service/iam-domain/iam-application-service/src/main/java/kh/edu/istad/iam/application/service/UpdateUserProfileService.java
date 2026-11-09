
package kh.edu.istad.iam.application.service;

import kh.edu.istad.iam.application.command.UpdateUserProfileCommand;
import kh.edu.istad.iam.application.port.out.UserRepository;
import kh.edu.istad.iam.application.result.UserResult;
import kh.edu.istad.iam.application.usecase.UpdateUserProfileUseCase;
import kh.edu.istad.iam.domain.User;
import org.springframework.security.core.GrantedAuthority;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public final class UpdateUserProfileService implements UpdateUserProfileUseCase {

    private final UserRepository users;

    public UpdateUserProfileService(UserRepository users) {
        this.users = users;
    }

    @Override
    public UserResult execute(UpdateUserProfileCommand command) {
        UUID userUuid = command.userUuid();

        User existingUser = users.findByUuid(userUuid)
                .orElseThrow(() -> new IllegalArgumentException("User was not found."));

        if (existingUser.isDeleted()) {
            throw new IllegalStateException("Cannot update a deleted user.");
        }

        String actor = command.updatedBy();

        if (actor == null || actor.isBlank()) {
            throw new IllegalArgumentException("updatedBy is required.");
        }

        Instant updatedAt = Instant.now();

        Set<String> authorities = existingUser.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        User updatedUser = User.builder()
                .id(existingUser.getId())
                .uuid(existingUser.getUuid())
                .username(existingUser.getUsername())
                .passwordHash(existingUser.getPasswordHash())

                .givenName(valueOrDefault(command.givenName(), existingUser.getGivenName()))
                .familyName(valueOrDefault(command.familyName(), existingUser.getFamilyName()))
                .middleName(valueOrDefault(command.middleName(), existingUser.getMiddleName()))
                .nickname(valueOrDefault(command.nickname(), existingUser.getNickname()))
                .gender(valueOrDefault(command.gender(), existingUser.getGender()))
                .birthdate(valueOrDefault(command.birthdate(), existingUser.getBirthdate()))
                .locale(valueOrDefault(command.locale(), existingUser.getLocale()))

                .profileUrl(valueOrDefault(command.profileUrl(), existingUser.getProfileUrl()))
                .pictureUrl(valueOrDefault(command.pictureUrl(), existingUser.getPictureUrl()))
                .websiteUrl(valueOrDefault(command.websiteUrl(), existingUser.getWebsiteUrl()))

                .email(existingUser.getEmail())
                .emailVerified(existingUser.isEmailVerified())
                .phoneNumber(valueOrDefault(command.phoneNumber(), existingUser.getPhoneNumber()))
                .phoneNumberVerified(existingUser.isPhoneNumberVerified())
                .zoneinfo(valueOrDefault(command.zoneinfo(), existingUser.getZoneinfo()))

                .addressStreetAddress(valueOrDefault(
                        command.addressStreetAddress(), existingUser.getAddressStreetAddress()))
                .addressLocality(valueOrDefault(
                        command.addressLocality(), existingUser.getAddressLocality()))
                .addressRegion(valueOrDefault(
                        command.addressRegion(), existingUser.getAddressRegion()))
                .addressPostalCode(valueOrDefault(
                        command.addressPostalCode(), existingUser.getAddressPostalCode()))
                .addressCountry(valueOrDefault(
                        command.addressCountry(), existingUser.getAddressCountry()))

                .enabled(existingUser.isEnabledFlag())
                .accountNonExpired(existingUser.isAccountNonExpired())
                .accountNonLocked(existingUser.isAccountNonLocked())
                .credentialsNonExpired(existingUser.isCredentialsNonExpired())
                .failedLoginAttempts(existingUser.getFailedLoginAttempts())
                .lastLoginAt(existingUser.getLastLoginAt())
                .passwordChangedAt(existingUser.getPasswordChangedAt())

                .createdAt(existingUser.getCreatedAt())
                .createdBy(existingUser.getCreatedBy())
                .updatedAt(updatedAt)
                .updatedBy(actor)

                .deleted(existingUser.isDeleted())
                .deletedAt(existingUser.getDeletedAt())
                .deletedBy(existingUser.getDeletedBy())

                .version(existingUser.getVersion() + 1)
                .authorities(authorities)
                .build();

        User savedUser = users.save(updatedUser);

        return toUserResult(savedUser);
    }

    private static <T> T valueOrDefault(T value, T defaultValue) {
        return value != null ? value : defaultValue;
    }

    private UserResult toUserResult(User user) {
        return new UserResult(
                user.getUuid(),
                user.getUsername(),
                user.getGivenName(),
                user.getFamilyName(),
                user.getMiddleName(),
                user.getNickname(),
                user.getGender(),
                user.getBirthdate(),
                user.getLocale(),
                user.getProfileUrl(),
                user.getPictureUrl(),
                user.getWebsiteUrl(),
                user.getEmail(),
                user.isEmailVerified(),
                user.getPhoneNumber(),
                user.isPhoneNumberVerified(),
                user.getZoneinfo(),
                user.getAddressStreetAddress(),
                user.getAddressLocality(),
                user.getAddressRegion(),
                user.getAddressPostalCode(),
                user.getAddressCountry(),
                user.isEnabledFlag(),
                user.isAccountNonExpired(),
                user.isAccountNonLocked(),
                user.isCredentialsNonExpired(),
                user.getFailedLoginAttempts(),
                user.getLastLoginAt(),
                user.getPasswordChangedAt(),
                user.getCreatedAt(),
                user.getCreatedBy(),
                user.getUpdatedAt(),
                user.getUpdatedBy(),
                user.isDeleted(),
                user.getDeletedAt(),
                user.getDeletedBy(),
                user.getVersion()
        );
    }
}
