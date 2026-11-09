package kh.edu.istad.iam;

import kh.edu.istad.iam.application.port.out.UserRepository;
import kh.edu.istad.iam.application.usecase.GetUserAuthoritiesUseCase;
import kh.edu.istad.iam.application.usecase.GetUserUseCase;
import kh.edu.istad.iam.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class IamServiceApplicationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository users;
    @Autowired private GetUserUseCase getUser;
    @Autowired private GetUserAuthoritiesUseCase getUserAuthorities;

    @Test
    void contextLoads() {
        assertThat(getUser).isNotNull();
        assertThat(getUserAuthorities).isNotNull();
    }

    @Test
    @Transactional
    void meReturnsUserForAuthenticatedBearerPrincipal() throws Exception {
        Instant now = Instant.now();
        User user = users.save(User.builder().uuid(UUID.randomUUID()).username("me-api-test")
                .passwordHash("{noop}unused").givenName("Me").familyName("Test").locale("en")
                .email("me-api-test@example.com").createdAt(now).createdBy("test")
                .updatedAt(now).updatedBy("test").build());

        mockMvc.perform(get("/api/me")
                        .with(jwt().jwt(token -> token.subject(user.getUuid().toString())))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid").value(user.getUuid().toString()))
                .andExpect(jsonPath("$.username").value("me-api-test"));
    }

    @Test
    void meRejectsMissingAndInvalidBearerTokens() throws Exception {
        mockMvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/me").header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());
    }

}
