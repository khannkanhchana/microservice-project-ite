package kh.edu.istad.iam.infrastructure.oauth;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.FactorGrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.security.Principal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * The authorization server stores the signed-in principal in oauth2_authorization.attributes and reads it back
 * at /oauth2/token. Social sign-ins (Google, GitHub, ...) store an OAuth2AuthenticationToken, which must survive
 * that round trip or the token exchange fails with 500.
 */
class SocialPrincipalJsonMapperTest {

    private static final TypeReference<Map<String, Object>> MAP = new TypeReference<>() { };

    @Test
    void roundTripsGoogleOidcPrincipal() throws Exception {
        String userUuid = UUID.randomUUID().toString();
        Instant now = Instant.now();
        Map<String, Object> idClaims = new LinkedHashMap<>();
        idClaims.put("iss", URI.create("https://accounts.google.com").toURL());
        idClaims.put("aud", new ArrayList<>(List.of("google-client-id.apps.googleusercontent.com")));
        idClaims.put("azp", "google-client-id.apps.googleusercontent.com");
        idClaims.put("sub", "109876543210987654321");
        idClaims.put("email", "someone@gmail.com");
        idClaims.put("email_verified", true);
        idClaims.put("at_hash", "abc");
        idClaims.put("nonce", "n-0S6_WzA2Mj");
        idClaims.put("name", "Some One");
        idClaims.put("picture", "https://lh3.googleusercontent.com/a/x");
        idClaims.put("given_name", "Some");
        idClaims.put("family_name", "One");
        idClaims.put("iat", now);
        idClaims.put("exp", now.plusSeconds(3600));
        OidcIdToken idToken = new OidcIdToken("id-token-value", now, now.plusSeconds(3600), idClaims);

        Map<String, Object> userInfo = new LinkedHashMap<>(idClaims);
        userInfo.put("iam_user_uuid", userUuid);
        List<GrantedAuthority> authorities = new ArrayList<>(List.of(new SimpleGrantedAuthority("ROLE_USER"),
                new SimpleGrantedAuthority("user:read")));
        authorities.add(new OidcUserAuthority(idToken));
        authorities.add(FactorGrantedAuthority.fromAuthority(FactorGrantedAuthority.AUTHORIZATION_CODE_AUTHORITY));
        DefaultOidcUser user = new DefaultOidcUser(authorities, idToken, new OidcUserInfo(userInfo), "iam_user_uuid");

        OAuth2AuthenticationToken restored = roundTrip(new OAuth2AuthenticationToken(user, authorities, "google"));
        assertEquals("google", restored.getAuthorizedClientRegistrationId());
        assertEquals(userUuid, restored.getName());
        assertInstanceOf(DefaultOidcUser.class, restored.getPrincipal());
        // The authorization server derives the ID token's auth_time from this authority.
        FactorGrantedAuthority factor = (FactorGrantedAuthority) restored.getAuthorities().stream()
                .filter(FactorGrantedAuthority.class::isInstance).findFirst().orElseThrow();
        assertEquals(FactorGrantedAuthority.AUTHORIZATION_CODE_AUTHORITY, factor.getAuthority());
    }

    @Test
    void roundTripsGithubOAuth2Principal() throws Exception {
        String userUuid = UUID.randomUUID().toString();
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("login", "octocat");
        attributes.put("id", 583231);
        attributes.put("node_id", "MDQ6VXNlcjU4MzIzMQ==");
        attributes.put("avatar_url", "https://avatars.githubusercontent.com/u/583231?v=4");
        attributes.put("site_admin", false);
        attributes.put("name", "The Octocat");
        attributes.put("company", null);
        attributes.put("email", "octocat@github.com");
        attributes.put("public_repos", 8);
        attributes.put("created_at", "2011-01-25T18:44:36Z");
        attributes.put("iam_user_uuid", userUuid);
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        OAuth2User user = new DefaultOAuth2User(authorities, attributes, "iam_user_uuid");

        OAuth2AuthenticationToken restored = roundTrip(new OAuth2AuthenticationToken(user, authorities, "github"));
        assertEquals(userUuid, restored.getName());
    }

    /** Same shape as JdbcOAuth2AuthorizationService: a Map<String, Object> attributes column. */
    private static OAuth2AuthenticationToken roundTrip(OAuth2AuthenticationToken token) {
        JsonMapper mapper = IamAuthorizationJsonMapper.create();
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put(Principal.class.getName(), token);
        attributes.put("iam_sid", UUID.randomUUID().toString());
        String json = mapper.writeValueAsString(attributes);
        Map<String, Object> restored = mapper.readValue(json, MAP);
        return assertInstanceOf(OAuth2AuthenticationToken.class, restored.get(Principal.class.getName()));
    }
}
