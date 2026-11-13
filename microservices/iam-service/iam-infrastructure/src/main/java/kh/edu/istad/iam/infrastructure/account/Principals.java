package kh.edu.istad.iam.infrastructure.account;

import kh.edu.istad.iam.infrastructure.security.IamUserDetails;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.UUID;

/** Resolves the IAM user UUID behind the principals produced by password and social login. */
public final class Principals {
    private Principals() { }

    public static UUID userUuid(Object principal) {
        if (principal instanceof IamUserDetails userDetails) return userDetails.uuid();
        if (principal instanceof OAuth2User oauthUser) {
            Object value = oauthUser.getAttributes().get("iam_user_uuid");
            return value == null ? null : parse(value.toString());
        }
        if (principal instanceof UserDetails userDetails) return parse(userDetails.getUsername());
        return null;
    }

    public static UUID parse(String value) {
        if (value == null) return null;
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
