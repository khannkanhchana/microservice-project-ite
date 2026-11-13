-- Self-service account security: device sessions, login history, security activity,
-- deactivation requests, uploaded profile pictures and password-reset tokens.

CREATE TABLE account_sessions (
    id UUID PRIMARY KEY,
    user_uuid UUID NOT NULL,
    login_method VARCHAR(30) NOT NULL,
    ip_address VARCHAR(64),
    user_agent VARCHAR(512),
    browser VARCHAR(60),
    os VARCHAR(60),
    device_type VARCHAR(20),
    location VARCHAR(120),
    created_at TIMESTAMPTZ NOT NULL,
    last_activity_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    revoked_reason VARCHAR(60)
);

CREATE INDEX ix_account_sessions_user ON account_sessions (user_uuid, revoked_at);

CREATE TABLE account_session_authorizations (
    authorization_id VARCHAR(100) PRIMARY KEY,
    session_id UUID NOT NULL REFERENCES account_sessions (id) ON DELETE CASCADE
);

CREATE INDEX ix_account_session_authorizations_session ON account_session_authorizations (session_id);

CREATE TABLE login_attempts (
    id UUID PRIMARY KEY,
    user_uuid UUID,
    identifier VARCHAR(255),
    success BOOLEAN NOT NULL,
    method VARCHAR(30) NOT NULL,
    failure_reason VARCHAR(255),
    ip_address VARCHAR(64),
    user_agent VARCHAR(512),
    browser VARCHAR(60),
    os VARCHAR(60),
    device_type VARCHAR(20),
    location VARCHAR(120),
    attempted_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX ix_login_attempts_user ON login_attempts (user_uuid, attempted_at DESC);

CREATE TABLE account_activities (
    id UUID PRIMARY KEY,
    user_uuid UUID NOT NULL,
    activity_type VARCHAR(50) NOT NULL,
    description VARCHAR(500),
    actor VARCHAR(255),
    ip_address VARCHAR(64),
    user_agent VARCHAR(512),
    browser VARCHAR(60),
    os VARCHAR(60),
    device_type VARCHAR(20),
    occurred_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX ix_account_activities_user ON account_activities (user_uuid, occurred_at DESC);

CREATE TABLE account_deactivation_requests (
    id UUID PRIMARY KEY,
    user_uuid UUID NOT NULL,
    reason VARCHAR(500),
    status VARCHAR(20) NOT NULL,
    requested_at TIMESTAMPTZ NOT NULL,
    processed_at TIMESTAMPTZ
);

CREATE INDEX ix_account_deactivation_requests_user ON account_deactivation_requests (user_uuid);

CREATE TABLE user_pictures (
    user_uuid UUID PRIMARY KEY,
    content_type VARCHAR(50) NOT NULL,
    data BYTEA NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE password_reset_tokens (
    token_hash VARCHAR(64) PRIMARY KEY,
    user_uuid UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ
);

CREATE INDEX ix_password_reset_tokens_user ON password_reset_tokens (user_uuid);
