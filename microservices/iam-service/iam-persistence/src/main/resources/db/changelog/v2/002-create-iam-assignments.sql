CREATE SEQUENCE IF NOT EXISTS iam_assignment_id_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS user_roles (
    id BIGINT NOT NULL DEFAULT nextval('iam_assignment_id_seq') PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    role_id BIGINT NOT NULL REFERENCES roles(id),
    assigned_at TIMESTAMPTZ NOT NULL,
    assigned_by VARCHAR(100) NOT NULL,
    expires_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ,
    revoked_by VARCHAR(100),
    reason VARCHAR(255),
    CONSTRAINT ck_user_roles_expiration CHECK (expires_at IS NULL OR expires_at > assigned_at),
    CONSTRAINT ck_user_roles_revocation CHECK (revoked_at IS NULL OR revoked_at >= assigned_at)
);

CREATE TABLE IF NOT EXISTS role_perms (
    id BIGINT NOT NULL DEFAULT nextval('iam_assignment_id_seq') PRIMARY KEY,
    role_id BIGINT NOT NULL REFERENCES roles(id),
    permission_id BIGINT NOT NULL REFERENCES permissions(id),
    assigned_at TIMESTAMPTZ NOT NULL,
    assigned_by VARCHAR(100) NOT NULL,
    expires_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ,
    revoked_by VARCHAR(100),
    reason VARCHAR(255),
    CONSTRAINT ck_role_perms_expiration CHECK (expires_at IS NULL OR expires_at > assigned_at),
    CONSTRAINT ck_role_perms_revocation CHECK (revoked_at IS NULL OR revoked_at >= assigned_at)
);

CREATE TABLE IF NOT EXISTS user_perms (
    id BIGINT NOT NULL DEFAULT nextval('iam_assignment_id_seq') PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    permission_id BIGINT NOT NULL REFERENCES permissions(id),
    effect VARCHAR(5) NOT NULL CHECK (effect IN ('GRANT', 'DENY')),
    assigned_at TIMESTAMPTZ NOT NULL,
    assigned_by VARCHAR(100) NOT NULL,
    expires_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ,
    revoked_by VARCHAR(100),
    reason VARCHAR(255),
    CONSTRAINT ck_user_perms_expiration CHECK (expires_at IS NULL OR expires_at > assigned_at),
    CONSTRAINT ck_user_perms_revocation CHECK (revoked_at IS NULL OR revoked_at >= assigned_at)
);

CREATE INDEX IF NOT EXISTS ix_user_roles_user ON user_roles (user_id, role_id);
CREATE INDEX IF NOT EXISTS ix_role_perms_role ON role_perms (role_id, permission_id);
CREATE INDEX IF NOT EXISTS ix_user_perms_user ON user_perms (user_id, permission_id);
