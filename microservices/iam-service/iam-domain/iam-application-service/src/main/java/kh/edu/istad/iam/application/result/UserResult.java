package kh.edu.istad.iam.application.result;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record UserResult(
        UUID uuid, String username, String givenName, String familyName, String middleName, String nickname,
        String gender, LocalDate birthdate, String locale, String profileUrl, String pictureUrl, String websiteUrl,
        String email, boolean emailVerified, String phoneNumber, boolean phoneNumberVerified, String zoneinfo,
        String addressStreetAddress, String addressLocality, String addressRegion, String addressPostalCode,
        String addressCountry, boolean enabled, boolean accountNonExpired, boolean accountNonLocked,
        boolean credentialsNonExpired, int failedLoginAttempts, Instant lastLoginAt, Instant passwordChangedAt,
        Instant createdAt, String createdBy, Instant updatedAt, String updatedBy, boolean deleted,
        Instant deletedAt, String deletedBy, long version) { }
