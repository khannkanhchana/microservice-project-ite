package kh.edu.istad.iam.infrastructure.security;

import kh.edu.istad.iam.application.port.out.CurrentActorProvider;
import kh.edu.istad.iam.domain.User;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
public class SecurityContextCurrentActorProvider implements CurrentActorProvider {
    @Override
    public String currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new IllegalStateException("No authenticated actor is available for audit attribution");
        }

        Object principal = authentication.getPrincipal();
        String actor;
        if (principal instanceof Jwt jwt) {
            actor = jwt.getSubject();
        } else if (principal instanceof OAuth2AuthenticatedPrincipal oauthPrincipal) {
            actor = oauthPrincipal.getName();
        } else if (principal instanceof User user) {
            actor = user.getSubject();
        } else if (principal instanceof UserDetails userDetails) {
            actor = userDetails.getUsername();
        } else {
            actor = authentication.getName();
        }
        if (actor == null || actor.isBlank()) {
            throw new IllegalStateException("Authenticated principal has no usable audit identifier");
        }
        return actor;
    }
}
