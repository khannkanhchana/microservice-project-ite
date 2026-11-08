package kh.edu.istad.iam.application.command;

import java.time.LocalDate;
import java.util.UUID;

public record UpdateUserProfileCommand(
        UUID userUuid, String givenName, String familyName, String middleName, String nickname,
        String gender, LocalDate birthdate, String locale, String profileUrl, String pictureUrl,
        String websiteUrl, String phoneNumber, String zoneinfo,
        String addressStreetAddress, String addressLocality, String addressRegion,
        String addressPostalCode, String addressCountry, String updatedBy) { }
