package kh.edu.istad.iam.web;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * Starts a social login on the same origin as the provider's registered callback.
 *
 * <p>The OAuth2 authorization request is kept in the HTTP session, and session cookies are per host.
 * Telegram's callback is registered on the public ngrok URL, so a login started on
 * {@code http://localhost:9000} comes back to a host without that session and fails with
 * {@code authorization_request_not_found}. For providers whose redirect-uri is a fixed absolute URL on
 * another origin, this sends the browser to that origin first so the whole flow shares one session.
 */
@Controller
public class SocialLoginRedirectController {
    private static final List<String> PROVIDERS = List.of("google", "github", "facebook", "telegram");

    private final Environment environment;

    public SocialLoginRedirectController(Environment environment) {
        this.environment = environment;
    }

    @GetMapping("/login/social/{provider}")
    public String start(@PathVariable("provider") String provider) {
        if (!PROVIDERS.contains(provider)) return "redirect:/login";
        String authorizationPath = "/oauth2/authorization/" + provider;

        String callbackOrigin = origin(environment.getProperty(
                "spring.security.oauth2.client.registration." + provider + ".redirect-uri"));
        String currentOrigin = ServletUriComponentsBuilder.fromCurrentContextPath()
                .replacePath(null).build().toUriString();

        if (callbackOrigin == null || callbackOrigin.equalsIgnoreCase(currentOrigin)) {
            return "redirect:" + authorizationPath;
        }
        return "redirect:" + callbackOrigin + authorizationPath;
    }

    /** Origin of an absolute redirect-uri; null for templated values such as {baseUrl}/login/oauth2/code/... */
    private static String origin(String redirectUri) {
        if (redirectUri == null || redirectUri.isBlank() || redirectUri.contains("{")) return null;
        try {
            URI uri = URI.create(redirectUri.trim());
            if (uri.getScheme() == null || uri.getHost() == null) return null;
            return uri.getScheme() + "://" + uri.getHost() + (uri.getPort() == -1 ? "" : ":" + uri.getPort());
        } catch (IllegalArgumentException invalid) {
            return null;
        }
    }
}
