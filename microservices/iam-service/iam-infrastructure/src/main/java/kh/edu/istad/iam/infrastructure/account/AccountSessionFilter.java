package kh.edu.istad.iam.infrastructure.account;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Ends browser sessions on the IAM server that the user revoked from another device, so a revoked
 * browser has to sign in again instead of silently obtaining new tokens; otherwise records activity.
 */
public class AccountSessionFilter extends OncePerRequestFilter {
    private final AccountSessionRegistry sessions;

    public AccountSessionFilter(AccountSessionRegistry sessions) {
        this.sessions = sessions;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session != null) {
            UUID sessionId = Principals.parse((String) session.getAttribute(AccountSessionRegistry.SESSION_ATTRIBUTE));
            if (sessionId != null) {
                if (sessions.isActive(sessionId)) {
                    sessions.touch(sessionId, session.getId());
                } else {
                    session.invalidate();
                    SecurityContextHolder.clearContext();
                }
            }
        }
        chain.doFilter(request, response);
    }
}
