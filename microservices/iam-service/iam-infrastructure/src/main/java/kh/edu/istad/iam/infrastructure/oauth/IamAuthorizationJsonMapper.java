package kh.edu.istad.iam.infrastructure.oauth;

import kh.edu.istad.iam.infrastructure.security.IamUserDetails;
import org.springframework.security.jackson.SecurityJacksonModules;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Jackson 3 mapper used only for persisted OAuth2 authorization data. */
public final class IamAuthorizationJsonMapper {
    private IamAuthorizationJsonMapper() { }

    public static JsonMapper create() {
        BasicPolymorphicTypeValidator.Builder validator = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType(IamUserDetails.class)
                // These exact JDK collection implementations are emitted by the existing
                // UserDetails and UsernamePasswordAuthenticationToken serialization.
                .allowIfSubType(List.of().getClass())
                .allowIfSubType(List.of("authority").getClass())
                .allowIfSubType(Collections.unmodifiableList(new ArrayList<>()).getClass());

        return JsonMapper.builder()
                .addModules(SecurityJacksonModules.getModules(IamAuthorizationJsonMapper.class.getClassLoader(), validator))
                .build();
    }
}
