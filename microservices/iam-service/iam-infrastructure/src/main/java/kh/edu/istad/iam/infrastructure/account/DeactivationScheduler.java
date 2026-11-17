package kh.edu.istad.iam.infrastructure.account;

import kh.edu.istad.iam.application.account.AccountSelfServiceUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/** Deactivates accounts whose deactivation request has passed its grace period (see AccountSelfServiceService). */
@Configuration
@EnableScheduling
class DeactivationScheduler {
    private static final Logger log = LoggerFactory.getLogger(DeactivationScheduler.class);

    private final AccountSelfServiceUseCase accounts;

    DeactivationScheduler(AccountSelfServiceUseCase accounts) {
        this.accounts = accounts;
    }

    @Scheduled(initialDelayString = "PT1M", fixedDelayString = "${iam.account.deactivation-check-interval:PT5M}")
    void completeDueDeactivations() {
        try {
            accounts.processDueDeactivations();
        } catch (RuntimeException exception) {
            log.warn("Scheduled account deactivation failed; it will be retried", exception);
        }
    }
}
