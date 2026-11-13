package kh.edu.istad.iam.infrastructure.account;

import jakarta.servlet.http.HttpSession;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

/**
 * Tags each OAuth2 authorization with the device session of the browser that approved it, so tokens
 * issued later (at the token endpoint, without the browser cookie) still know which session they belong to.
 */
public class SessionAwareAuthorizationService implements OAuth2AuthorizationService {
    private final OAuth2AuthorizationService delegate;
    private final AccountSessionRegistry sessions;

    public SessionAwareAuthorizationService(OAuth2AuthorizationService delegate, AccountSessionRegistry sessions) {
        this.delegate = delegate;
        this.sessions = sessions;
    }

    @Override
    public void save(OAuth2Authorization authorization) {
        OAuth2Authorization toSave = authorization;
        if (authorization.getAttribute(AccountSessionRegistry.AUTHORIZATION_ATTRIBUTE) == null) {
            UUID sessionId = currentBrowserSession();
            if (sessionId != null) {
                toSave = OAuth2Authorization.from(authorization)
                        .attribute(AccountSessionRegistry.AUTHORIZATION_ATTRIBUTE, sessionId.toString())
                        .build();
            }
        }
        delegate.save(toSave);
        UUID sessionId = Principals.parse(toSave.getAttribute(AccountSessionRegistry.AUTHORIZATION_ATTRIBUTE));
        if (sessionId != null) sessions.bindAuthorization(toSave.getId(), sessionId);
    }

    @Override
    public void remove(OAuth2Authorization authorization) {
        delegate.remove(authorization);
    }

    @Override
    public OAuth2Authorization findById(String id) {
        return delegate.findById(id);
    }

    @Override
    public OAuth2Authorization findByToken(String token, OAuth2TokenType tokenType) {
        return delegate.findByToken(token, tokenType);
    }

    private static UUID currentBrowserSession() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) return null;
        HttpSession session = attributes.getRequest().getSession(false);
        if (session == null) return null;
        Object value = session.getAttribute(AccountSessionRegistry.SESSION_ATTRIBUTE);
        return value == null ? null : Principals.parse(value.toString());
    }
}
