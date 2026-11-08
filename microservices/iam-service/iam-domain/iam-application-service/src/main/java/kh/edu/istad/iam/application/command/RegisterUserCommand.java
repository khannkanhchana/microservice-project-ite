package kh.edu.istad.iam.application.command;

import java.time.LocalDate;

public record RegisterUserCommand(
        String username, String password, String givenName, String familyName, String locale, String email,
        String middleName, String nickname, String gender, LocalDate birthdate,
        String profileUrl, String pictureUrl, String websiteUrl,
        String phoneNumber, String zoneinfo,
        String addressStreetAddress, String addressLocality, String addressRegion,
        String addressPostalCode, String addressCountry) { }
