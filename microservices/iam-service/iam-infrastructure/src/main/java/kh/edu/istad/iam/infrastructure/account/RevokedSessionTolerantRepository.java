package kh.edu.istad.iam.infrastructure.account;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.session.data.redis.RedisSessionRepository;
import org.springframework.stereotype.Component;

/**
 * Lets an in-flight request finish quietly when its browser session was deleted from Redis meanwhile.
 *
 * <p>{@link AccountSessionRegistry} signs a browser out by deleting its Spring Session from Redis
 * (session revoked, "log out all devices", password reset, account deactivated/locked/disabled). A request
 * from that browser that is still running then reaches the end of the request and
 * {@link RedisSessionRepository#save} throws {@code IllegalStateException("Session was invalidated")},
 * turning an ordinary sign-out into a 500. The session was ended on purpose, so not saving it — and not
 * bringing it back — is the correct outcome.
 */
@Component
class RevokedSessionTolerantRepository implements BeanPostProcessor {

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        return bean instanceof RedisSessionRepository repository ? new Tolerant<>(repository) : bean;
    }

    static final class Tolerant<S extends Session> implements SessionRepository<S> {
        private static final Logger log = LoggerFactory.getLogger(RevokedSessionTolerantRepository.class);
        private static final String INVALIDATED = "Session was invalidated";

        private final SessionRepository<S> delegate;

        Tolerant(SessionRepository<S> delegate) {
            this.delegate = delegate;
        }

        @Override
        public S createSession() {
            return delegate.createSession();
        }

        @Override
        public void save(S session) {
            try {
                delegate.save(session);
            } catch (IllegalStateException exception) {
                if (!INVALIDATED.equals(exception.getMessage())) throw exception;
                log.debug("Not saving HTTP session {}: it was ended while the request was running", session.getId());
            }
        }

        @Override
        public S findById(String id) {
            return delegate.findById(id);
        }

        @Override
        public void deleteById(String id) {
            delegate.deleteById(id);
        }
    }
}
