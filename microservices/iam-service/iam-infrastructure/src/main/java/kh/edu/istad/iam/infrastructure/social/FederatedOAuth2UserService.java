package kh.edu.istad.iam.infrastructure.social;

import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class FederatedOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {
    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
    private final FederatedUserProvisioner provisioner;

    public FederatedOAuth2UserService(FederatedUserProvisioner provisioner) {
        this.provisioner = provisioner;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest request) {
        OAuth2User providerUser = delegate.loadUser(request);
        Map<String, Object> attributes = new LinkedHashMap<>(providerUser.getAttributes());
        FederatedUserProvisioner.ProvisionedUser provisioned = provisioner.provision(
                request.getClientRegistration().getRegistrationId(), attributes);
        attributes.put("iam_user_uuid", provisioned.user().uuid().toString());
        return new DefaultOAuth2User(provisioned.authorities(), attributes, "iam_user_uuid");
    }
}
