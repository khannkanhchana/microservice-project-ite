package kh.edu.istad.iam.infrastructure.oauth;

import kh.edu.istad.iam.application.result.AuthUserResult;
import kh.edu.istad.iam.infrastructure.security.IamUserDetails;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import tools.jackson.databind.json.JsonMapper;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IamAuthorizationJsonMapperTest {

    @Test
    void roundTripsAuthorizationSecurityContextWithIamPrincipal() {
        JsonMapper mapper = IamAuthorizationJsonMapper.create();
        UUID userUuid = UUID.randomUUID();
        IamUserDetails principal = new IamUserDetails(new AuthUserResult(
                userUuid, "postman-user", "{bcrypt}encoded-hash", true, true, true, true,
                Set.of("user:read")));
        SecurityContext context = new SecurityContextImpl(new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities()));

        String json = mapper.writeValueAsString(new SecurityContextEnvelope(context));
        SecurityContext restored = mapper.readValue(json, SecurityContextEnvelope.class).securityContext();
        IamUserDetails restoredPrincipal = assertInstanceOf(
                IamUserDetails.class, restored.getAuthentication().getPrincipal());
        assertEquals(userUuid, restoredPrincipal.uuid());
        assertEquals("postman-user", restoredPrincipal.getUsername());
        assertEquals("{bcrypt}encoded-hash", restoredPrincipal.getPassword());
        assertEquals(Set.of("user:read"), restoredPrincipal.getAuthorities().stream()
                .map(authority -> authority.getAuthority()).collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    void rejectsApplicationTypesNotExplicitlyAllowedByValidator() {
        JsonMapper mapper = IamAuthorizationJsonMapper.create();
        String json = mapper.writeValueAsString(new ObjectEnvelope(new UntrustedPayload("value")));

        assertThrows(RuntimeException.class, () -> mapper.readValue(json, ObjectEnvelope.class));
    }

    private record SecurityContextEnvelope(SecurityContext securityContext) { }

    private record ObjectEnvelope(Object payload) { }

    private record UntrustedPayload(String value) { }
}
