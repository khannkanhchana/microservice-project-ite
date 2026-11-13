package kh.edu.istad.iam.infrastructure.social;

import org.springframework.security.oauth2.client.oidc.authentication.OidcIdTokenDecoderFactory;
import org.springframework.security.oauth2.client.oidc.authentication.OidcIdTokenValidator;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoderFactory;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ID-token decoder for social login. Equivalent to Spring's {@link OidcIdTokenDecoderFactory}
 * (same validators and claim conversion), but fetches the provider's JWK set through a client that
 * tolerates gzip bodies (see {@link GzipTolerantInterceptor}), and accepts ES256 as well as RS256
 * since Telegram advertises both. Picked up by {@code oauth2Login()} as the
 * {@code JwtDecoderFactory<ClientRegistration>} bean.
 */
@Component
public class SocialIdTokenDecoderFactory implements JwtDecoderFactory<ClientRegistration> {
    private final RestTemplate restTemplate = new RestTemplate();
    private final Map<String, JwtDecoder> decoders = new ConcurrentHashMap<>();

    public SocialIdTokenDecoderFactory() {
        restTemplate.getInterceptors().add(new GzipTolerantInterceptor());
    }

    @Override
    public JwtDecoder createDecoder(ClientRegistration registration) {
        return decoders.computeIfAbsent(registration.getRegistrationId(), id -> build(registration));
    }

    private JwtDecoder build(ClientRegistration registration) {
        String jwkSetUri = registration.getProviderDetails().getJwkSetUri();
        if (jwkSetUri == null || jwkSetUri.isBlank()) {
            throw new OAuth2AuthenticationException(new OAuth2Error("missing_signature_verifier",
                    "Failed to find a Signature Verifier for Client Registration: '"
                            + registration.getRegistrationId() + "'. Check to ensure you have configured the JwkSet URI.",
                    null));
        }
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri)
                .jwsAlgorithms(algorithms -> {
                    algorithms.add(SignatureAlgorithm.RS256);
                    algorithms.add(SignatureAlgorithm.ES256);
                })
                .restOperations(restTemplate)
                .build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(), new OidcIdTokenValidator(registration)));
        decoder.setClaimSetConverter(OidcIdTokenDecoderFactory.createDefaultClaimTypeConverter());
        return decoder;
    }
}
