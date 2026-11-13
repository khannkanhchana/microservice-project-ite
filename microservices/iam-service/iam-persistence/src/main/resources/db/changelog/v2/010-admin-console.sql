-- Administration console: system audit log, runtime system settings, timed account lockout,
-- and the system permissions that guard the admin API (granted to the ADMIN role).

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    occurred_at TIMESTAMPTZ NOT NULL,
    category VARCHAR(40) NOT NULL,
    action VARCHAR(60) NOT NULL,
    outcome VARCHAR(20) NOT NULL,
    actor VARCHAR(255),
    actor_uuid UUID,
    target_type VARCHAR(40),
    target_id VARCHAR(255),
    description VARCHAR(1000),
    details TEXT,
    ip_address VARCHAR(64),
    user_agent VARCHAR(512),
    http_method VARCHAR(10),
    path VARCHAR(500),
    status INTEGER
);

CREATE INDEX ix_audit_logs_time ON audit_logs (occurred_at DESC);
CREATE INDEX ix_audit_logs_category ON audit_logs (category, occurred_at DESC);
CREATE INDEX ix_audit_logs_actor ON audit_logs (actor_uuid, occurred_at DESC);
CREATE INDEX ix_audit_logs_target ON audit_logs (target_id, occurred_at DESC);

CREATE TABLE system_settings (
    setting_key VARCHAR(100) PRIMARY KEY,
    setting_value TEXT NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(255) NOT NULL
);

ALTER TABLE users ADD COLUMN IF NOT EXISTS lock_expires_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS ix_login_attempts_time ON login_attempts (attempted_at DESC);
CREATE INDEX IF NOT EXISTS ix_login_attempts_ip ON login_attempts (ip_address, attempted_at DESC);

INSERT INTO permissions (uuid, name, resource, action, description, is_system, is_active, created_at, created_by,
                         updated_at, updated_by)
SELECT gen_random_uuid(), p.resource || ':' || p.action, p.resource, p.action, p.description, TRUE, TRUE,
       now(), 'system', now(), 'system'
FROM (VALUES
    ('dashboard', 'read', 'View the administration overview'),
    ('user', 'read', 'View user accounts'),
    ('user', 'create', 'Create user accounts'),
    ('user', 'update', 'Update, activate, deactivate, lock and unlock user accounts'),
    ('user', 'delete', 'Delete user accounts'),
    ('user', 'assign', 'Assign roles and permissions to users'),
    ('role', 'read', 'View roles'),
    ('role', 'create', 'Create roles'),
    ('role', 'update', 'Update roles'),
    ('role', 'delete', 'Delete roles'),
    ('role', 'assign', 'Assign permissions to roles'),
    ('permission', 'read', 'View permissions'),
    ('permission', 'create', 'Create permissions'),
    ('permission', 'update', 'Update permissions'),
    ('permission', 'delete', 'Delete permissions'),
    ('client', 'read', 'View OAuth2/OIDC clients'),
    ('client', 'create', 'Register OAuth2/OIDC clients'),
    ('client', 'update', 'Update OAuth2/OIDC clients and rotate secrets'),
    ('client', 'delete', 'Delete OAuth2/OIDC clients'),
    ('session', 'read', 'View users'' active sessions and login history'),
    ('session', 'revoke', 'Terminate users'' sessions'),
    ('audit', 'read', 'View audit logs'),
    ('settings', 'read', 'View system configuration'),
    ('settings', 'update', 'Change system configuration')
) AS p(resource, action, description)
WHERE NOT EXISTS (SELECT 1 FROM permissions x WHERE x.resource = p.resource AND x.action = p.action);

UPDATE permissions SET is_system = TRUE, is_active = TRUE, is_deleted = FALSE, deleted_at = NULL, deleted_by = NULL
WHERE name IN ('dashboard:read', 'user:read', 'user:create', 'user:update', 'user:delete', 'user:assign',
               'role:read', 'role:create', 'role:update', 'role:delete', 'role:assign',
               'permission:read', 'permission:create', 'permission:update', 'permission:delete',
               'client:read', 'client:create', 'client:update', 'client:delete',
               'session:read', 'session:revoke', 'audit:read', 'settings:read', 'settings:update');

INSERT INTO roles (uuid, name, description, is_system, is_active, created_at, created_by, updated_at, updated_by)
SELECT gen_random_uuid(), 'ADMIN', 'System administrator', TRUE, TRUE, now(), 'system', now(), 'system'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ADMIN');

UPDATE roles SET is_system = TRUE, is_active = TRUE, is_deleted = FALSE, deleted_at = NULL, deleted_by = NULL
WHERE name = 'ADMIN';

INSERT INTO role_perms (role_id, permission_id, assigned_at, assigned_by, reason)
SELECT r.id, p.id, now(), 'system', 'Administration console'
FROM roles r
JOIN permissions p ON p.is_system AND p.resource IN
    ('dashboard', 'user', 'role', 'permission', 'client', 'session', 'audit', 'settings')
WHERE r.name = 'ADMIN'
  AND NOT EXISTS (SELECT 1 FROM role_perms rp
                  WHERE rp.role_id = r.id AND rp.permission_id = p.id AND rp.revoked_at IS NULL);
