package kh.edu.istad.iam.infrastructure.social;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.FactorGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class FederatedOidcUserService extends org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService {
    private final FederatedUserProvisioner provisioner;

    public FederatedOidcUserService(FederatedUserProvisioner provisioner) {
        this.provisioner = provisioner;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest request) {
        if ("telegram".equals(request.getClientRegistration().getRegistrationId())
                && (request.getIdToken().getIssuer() == null
                || !"https://oauth.telegram.org".equals(request.getIdToken().getIssuer().toString()))) {
            throw new OAuth2AuthenticationException(new OAuth2Error("invalid_id_token", "Unexpected Telegram issuer.", null));
        }
        Map<String, Object> claims = new LinkedHashMap<>(request.getIdToken().getClaims());
        FederatedUserProvisioner.ProvisionedUser provisioned = provisioner.provision(
                request.getClientRegistration().getRegistrationId(), claims);
        claims.put("iam_user_uuid", provisioned.user().uuid().toString());

        List<GrantedAuthority> authorities = new ArrayList<>(provisioned.authorities());
        authorities.add(new OidcUserAuthority(request.getIdToken()));
        // Spring Security 7 records how and when a user authenticated as a FactorGrantedAuthority; the
        // authorization server reads it for the ID token's auth_time claim. OAuth2LoginAuthenticationProvider
        // adds it for plain OAuth2 logins (GitHub, Facebook), but the OIDC provider (Google, Telegram) does not,
        // so without it /oauth2/token fails with "authenticationTime cannot be null".
        authorities.add(FactorGrantedAuthority.fromAuthority(FactorGrantedAuthority.AUTHORIZATION_CODE_AUTHORITY));
        return new DefaultOidcUser(authorities, request.getIdToken(), new OidcUserInfo(claims), "iam_user_uuid");
    }
}
