package kh.edu.istad.iam.restapi.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class UserRequests {
    private UserRequests() { }

    public record Registration(
            @NotBlank @Size(max = 50) String username,
            @NotBlank @Size(min = 8, max = 100) String password,
            @NotBlank @Size(min = 8, max = 100) String confirmPassword,
            @NotBlank @Size(max = 100) String givenName,
            @NotBlank @Size(max = 100) String familyName,
            @NotBlank @Size(max = 10) String locale,
            @NotBlank @Email @Size(max = 255) String email) { }

    public record CreateUser(
            @NotBlank @Size(max = 50) String username,
            @NotBlank @Email @Size(max = 255) String email,
            @NotBlank @Size(min = 8, max = 100) String password,
            @NotBlank @Size(max = 100) String givenName,
            @NotBlank @Size(max = 100) String familyName,
            String middleName, String nickname, String gender, LocalDate birthdate,
            String phoneNumber, String zoneinfo,
            @NotBlank @Size(max = 10) String locale,
            String profileUrl, String pictureUrl, String websiteUrl,
            Address address, List<UUID> roleUuids) {
        public CreateUser {
            roleUuids = roleUuids == null ? List.of() : List.copyOf(roleUuids);
        }
    }

    public record Address(String streetAddress, String locality, String region,
                          String postalCode, String country) { }

    public record Profile(
            @Size(min = 3, max = 50) String username,
            @Email @Size(max = 255) String email,
            @Size(max = 100) String givenName,
            @Size(max = 100) String familyName,
            String middleName, String nickname, String gender, LocalDate birthdate,
            @Size(max = 10) String locale, String profileUrl, String pictureUrl,
            String websiteUrl, String phoneNumber, String zoneinfo,
            String addressStreetAddress, String addressLocality, String addressRegion,
            String addressPostalCode, String addressCountry) { }

    /** currentPassword may be omitted only by accounts that have never had a password (social sign-up). */
    public record PasswordChange(String currentPassword,
                                 @NotBlank @Size(min = 8, max = 100) String newPassword,
                                 @NotBlank @Size(min = 8, max = 100) String confirmPassword,
                                 Boolean signOutOtherSessions) { }

    public record ForgotPassword(@NotBlank @Email @Size(max = 255) String email) { }

    public record ResetPassword(@NotBlank String token,
                                @NotBlank @Size(min = 8, max = 100) String newPassword,
                                @NotBlank @Size(min = 8, max = 100) String confirmPassword) { }

    public record Deactivation(String password, @Size(max = 500) String reason,
                               @NotBlank String confirmation) { }

    public record LinkIdentity(@NotBlank String provider, @NotBlank @Size(max = 500) String returnUrl) { }

    public record PasswordReset(@NotBlank @Size(min = 8, max = 100) String newPassword) { }

    public record RoleAssignments(@NotEmpty List<UUID> roleUuids,
                                  java.time.Instant expiresAt, @Size(max = 255) String reason) { }

    public record PermissionGrants(@NotEmpty List<UUID> permissionUuids,
                                   String effect, java.time.Instant expiresAt,
                                   @Size(max = 255) String reason) { }
}
