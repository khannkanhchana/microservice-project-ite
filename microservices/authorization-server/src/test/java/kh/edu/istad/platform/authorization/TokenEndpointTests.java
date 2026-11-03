package kh.edu.istad.platform.authorization;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "authorization.client.id=test-client",
        "authorization.client.secret=test-secret",
        "authorization.client.access-token-time-to-live=10m",
        "authorization.business-client.id=business-client",
        "authorization.business-client.secret=business-secret"
})
@AutoConfigureMockMvc
class TokenEndpointTests {
    @Autowired
    MockMvc mvc;

    private final ObjectMapper json = new ObjectMapper();

    @Test
    void rejectsUnknownClient() throws Exception {
        mvc.perform(post("/oauth2/token").with(httpBasic("unknown-client", "test-secret"))
                        .param("grant_type", "client_credentials"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("invalid_client"));
    }

    @Test
    void businessClientCanOnlyRequestBusinessRead() throws Exception {
        mvc.perform(post("/oauth2/token").with(httpBasic("business-client", "business-secret"))
                        .param("grant_type", "client_credentials").param("scope", "business.read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scope").value("business.read"));
        mvc.perform(post("/oauth2/token").with(httpBasic("business-client", "business-secret"))
                        .param("grant_type", "client_credentials").param("scope", "customer.read"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_scope"));
    }

    @Test
    void issuesJwtVerifiableWithPublishedPublicKey() throws Exception {
        var response = mvc.perform(post("/oauth2/token")
                        .with(httpBasic("test-client", "test-secret"))
                        .param("grant_type", "client_credentials")
                        .param("scope", "customer.read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.scope").value("customer.read"))
                .andExpect(jsonPath("$.refresh_token").doesNotExist())
                .andReturn().getResponse();
        var body = json.readTree(response.getContentAsString());
        assertThat(body.get("expires_in").asInt()).isBetween(599, 600);
        var jwt = SignedJWT.parse(body.get("access_token").asText());
        var claims = jwt.getJWTClaimsSet();
        assertThat(claims.getIssuer()).isEqualTo("http://localhost:9000");
        assertThat(claims.getSubject()).isEqualTo("test-client");
        assertThat(claims.getStringListClaim("scope")).containsExactly("customer.read");
        assertThat(claims.getExpirationTime().toInstant()
                .minusSeconds(600)).isEqualTo(claims.getIssueTime().toInstant());

        var keys = mvc.perform(get("/oauth2/jwks"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        var key = (RSAKey) JWKSet.parse(keys).getKeyByKeyId(jwt.getHeader().getKeyID());
        assertThat(key.isPrivate()).isFalse();
        assertThat(jwt.getHeader().getAlgorithm().getName()).isEqualTo("RS256");
        assertThat(jwt.verify(new RSASSAVerifier(key))).isTrue();
    }

    @Test
    void rejectsWrongSecret() throws Exception {
        mvc.perform(post("/oauth2/token").with(httpBasic("test-client", "wrong"))
                        .param("grant_type", "client_credentials"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("invalid_client"));
    }

    @Test
    void rejectsMissingAuthentication() throws Exception {
        mvc.perform(post("/oauth2/token").param("grant_type", "client_credentials"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsUnregisteredScope() throws Exception {
        mvc.perform(post("/oauth2/token").with(httpBasic("test-client", "test-secret"))
                        .param("grant_type", "client_credentials").param("scope", "customer.write"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_scope"));
    }

    @Test
    void rejectsPasswordGrant() throws Exception {
        mvc.perform(post("/oauth2/token").with(httpBasic("test-client", "test-secret"))
                        .param("grant_type", "password"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("unsupported_grant_type"));
    }

    @Test
    void rejectsClientSecretPost() throws Exception {
        mvc.perform(post("/oauth2/token").param("grant_type", "client_credentials")
                        .param("client_id", "test-client").param("client_secret", "test-secret"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("invalid_client"));
    }
}
