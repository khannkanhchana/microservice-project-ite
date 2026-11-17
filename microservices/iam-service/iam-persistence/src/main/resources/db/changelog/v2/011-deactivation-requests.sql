-- Account deactivation becomes a request with a grace period: PENDING until scheduled_for, when it is
-- COMPLETED (account disabled, sessions ended); the user can cancel it (CANCELLED) before then.

ALTER TABLE account_deactivation_requests ADD COLUMN scheduled_for TIMESTAMPTZ;
ALTER TABLE account_deactivation_requests ADD COLUMN cancelled_at TIMESTAMPTZ;

CREATE INDEX ix_account_deactivation_requests_due
    ON account_deactivation_requests (scheduled_for) WHERE status = 'PENDING';
