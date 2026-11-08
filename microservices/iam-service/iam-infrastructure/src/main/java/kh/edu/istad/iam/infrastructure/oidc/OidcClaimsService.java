package kh.edu.istad.iam.infrastructure.oidc;

import kh.edu.istad.iam.application.result.UserResult;
import kh.edu.istad.iam.application.usecase.GetUserUseCase;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
public class OidcClaimsService {
    private final ObjectProvider<GetUserUseCase> getUser;

    public OidcClaimsService(ObjectProvider<GetUserUseCase> getUser) {
        this.getUser = getUser;
    }

    public Map<String, Object> claims(UUID userUuid, Set<String> scopes) {
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("sub", userUuid.toString());
        GetUserUseCase useCase = getUser.getIfAvailable();
        if (useCase == null) return claims;
        UserResult user = useCase.execute(userUuid);

        if (scopes.contains("profile")) {
            put(claims, "name", fullName(user));
            put(claims, "given_name", user.givenName());
            put(claims, "family_name", user.familyName());
            put(claims, "middle_name", user.middleName());
            put(claims, "nickname", user.nickname());
            put(claims, "preferred_username", user.username());
            put(claims, "profile", user.profileUrl());
            put(claims, "picture", user.pictureUrl());
            put(claims, "website", user.websiteUrl());
            put(claims, "gender", user.gender());
            put(claims, "birthdate", user.birthdate() == null ? null : user.birthdate().toString());
            put(claims, "zoneinfo", user.zoneinfo());
            put(claims, "locale", user.locale());
            put(claims, "updated_at", user.updatedAt() == null ? null : user.updatedAt().getEpochSecond());
        }
        if (scopes.contains("email")) {
            put(claims, "email", user.email());
            claims.put("email_verified", user.emailVerified());
        }
        if (scopes.contains("phone")) {
            put(claims, "phone_number", user.phoneNumber());
            claims.put("phone_number_verified", user.phoneNumberVerified());
        }
        if (scopes.contains("address")) {
            Map<String, Object> address = new LinkedHashMap<>();
            put(address, "street_address", user.addressStreetAddress());
            put(address, "locality", user.addressLocality());
            put(address, "region", user.addressRegion());
            put(address, "postal_code", user.addressPostalCode());
            put(address, "country", user.addressCountry());
            if (!address.isEmpty()) claims.put("address", Map.copyOf(address));
        }
        return Map.copyOf(claims);
    }

    private static String fullName(UserResult user) {
        return java.util.stream.Stream.of(user.givenName(), user.familyName())
                .filter(value -> value != null && !value.isBlank())
                .collect(java.util.stream.Collectors.joining(" "));
    }

    private static void put(Map<String, Object> map, String key, Object value) {
        if (value != null) map.put(key, value);
    }
}
