ALTER TABLE users RENAME COLUMN password TO password_hash;
ALTER TABLE users RENAME COLUMN picture TO picture_url;
ALTER TABLE users RENAME COLUMN is_account_non_expired TO account_non_expired;
ALTER TABLE users RENAME COLUMN is_account_non_locked TO account_non_locked;
ALTER TABLE users RENAME COLUMN is_credentials_non_expired TO credentials_non_expired;
ALTER TABLE users RENAME COLUMN is_enabled TO enabled;

UPDATE users
SET uuid = CASE
        WHEN uuid ~* '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$' THEN uuid
        ELSE gen_random_uuid()::text
    END,
    username = COALESCE(NULLIF(left(btrim(username), 50), ''), 'user-' || id),
    email = COALESCE(NULLIF(left(lower(btrim(email)), 255), ''), 'legacy-' || id || '@invalid.local'),
    given_name = COALESCE(NULLIF(left(btrim(given_name), 100), ''), NULLIF(left(btrim(username), 100), ''), 'User'),
    family_name = COALESCE(NULLIF(left(btrim(family_name), 100), ''), 'Legacy'),
    email_verified = COALESCE(email_verified, FALSE),
    phone_number_verified = COALESCE(phone_number_verified, FALSE);

ALTER TABLE users ALTER COLUMN uuid TYPE UUID USING uuid::uuid;
ALTER TABLE users ALTER COLUMN uuid SET NOT NULL;
ALTER TABLE users ALTER COLUMN username TYPE VARCHAR(50);
ALTER TABLE users ALTER COLUMN username SET NOT NULL;
ALTER TABLE users ALTER COLUMN email TYPE VARCHAR(255);
ALTER TABLE users ALTER COLUMN email SET NOT NULL;
ALTER TABLE users ALTER COLUMN given_name TYPE VARCHAR(100);
ALTER TABLE users ALTER COLUMN given_name SET NOT NULL;
ALTER TABLE users ALTER COLUMN family_name TYPE VARCHAR(100);
ALTER TABLE users ALTER COLUMN family_name SET NOT NULL;
ALTER TABLE users ADD COLUMN middle_name VARCHAR(100);
ALTER TABLE users ADD COLUMN nickname VARCHAR(100);
ALTER TABLE users ADD COLUMN locale VARCHAR(10) NOT NULL DEFAULT 'en';
ALTER TABLE users ADD COLUMN profile_url VARCHAR(500);
ALTER TABLE users ADD COLUMN website_url VARCHAR(500);
ALTER TABLE users ADD COLUMN zoneinfo VARCHAR(50);
ALTER TABLE users ADD COLUMN address_street_address VARCHAR(255);
ALTER TABLE users ADD COLUMN address_locality VARCHAR(100);
ALTER TABLE users ADD COLUMN address_region VARCHAR(100);
ALTER TABLE users ADD COLUMN address_postal_code VARCHAR(20);
ALTER TABLE users ADD COLUMN address_country CHAR(2);
ALTER TABLE users ADD COLUMN failed_login_attempts INTEGER NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN last_login_at TIMESTAMPTZ;
ALTER TABLE users ADD COLUMN password_changed_at TIMESTAMPTZ;
ALTER TABLE users ADD COLUMN created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE users ADD COLUMN created_by VARCHAR(255) NOT NULL DEFAULT 'legacy-migration';
ALTER TABLE users ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE users ADD COLUMN updated_by VARCHAR(255) NOT NULL DEFAULT 'legacy-migration';
ALTER TABLE users ADD COLUMN is_deleted BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE users ADD COLUMN deleted_at TIMESTAMPTZ;
ALTER TABLE users ADD COLUMN deleted_by VARCHAR(255);
ALTER TABLE users ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
CREATE UNIQUE INDEX uq_users_uuid ON users(uuid);
CREATE UNIQUE INDEX uq_users_username ON users(username);
CREATE UNIQUE INDEX uq_users_email ON users(email);
CREATE UNIQUE INDEX uq_users_phone_number ON users(phone_number) WHERE phone_number IS NOT NULL;

ALTER TABLE roles RENAME COLUMN role TO name;
UPDATE roles SET name = COALESCE(NULLIF(left(btrim(name), 50), ''), 'LEGACY_' || id);
ALTER TABLE roles ALTER COLUMN name TYPE VARCHAR(50);
ALTER TABLE roles ALTER COLUMN name SET NOT NULL;
ALTER TABLE roles ADD COLUMN uuid UUID NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE roles ADD COLUMN is_system BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE roles ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE roles ADD COLUMN created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE roles ADD COLUMN created_by VARCHAR(255) NOT NULL DEFAULT 'legacy-migration';
ALTER TABLE roles ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE roles ADD COLUMN updated_by VARCHAR(255) NOT NULL DEFAULT 'legacy-migration';
ALTER TABLE roles ADD COLUMN is_deleted BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE roles ADD COLUMN deleted_at TIMESTAMPTZ;
ALTER TABLE roles ADD COLUMN deleted_by VARCHAR(255);
ALTER TABLE roles ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
CREATE UNIQUE INDEX uq_roles_uuid ON roles(uuid);
CREATE UNIQUE INDEX uq_roles_name ON roles(name);

ALTER TABLE permissions RENAME COLUMN permission TO name;
ALTER TABLE permissions ADD COLUMN resource VARCHAR(50);
ALTER TABLE permissions ADD COLUMN action VARCHAR(50);
UPDATE permissions
SET resource = left(CASE WHEN position(':' IN name) > 0 THEN split_part(name, ':', 1) ELSE 'legacy' END, 50),
    action = left(CASE WHEN position(':' IN name) > 0 THEN split_part(name, ':', 2) ELSE COALESCE(name, 'unknown') END, 50);
UPDATE permissions SET name = resource || ':' || action;
ALTER TABLE permissions ALTER COLUMN name TYPE VARCHAR(100);
ALTER TABLE permissions ALTER COLUMN name SET NOT NULL;
ALTER TABLE permissions ALTER COLUMN resource SET NOT NULL;
ALTER TABLE permissions ALTER COLUMN action SET NOT NULL;
ALTER TABLE permissions ADD COLUMN uuid UUID NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE permissions ADD COLUMN is_system BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE permissions ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE permissions ADD COLUMN created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE permissions ADD COLUMN created_by VARCHAR(255) NOT NULL DEFAULT 'legacy-migration';
ALTER TABLE permissions ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE permissions ADD COLUMN updated_by VARCHAR(255) NOT NULL DEFAULT 'legacy-migration';
ALTER TABLE permissions ADD COLUMN is_deleted BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE permissions ADD COLUMN deleted_at TIMESTAMPTZ;
ALTER TABLE permissions ADD COLUMN deleted_by VARCHAR(255);
ALTER TABLE permissions ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
CREATE UNIQUE INDEX uq_permissions_uuid ON permissions(uuid);
CREATE UNIQUE INDEX uq_permissions_name ON permissions(name);
CREATE UNIQUE INDEX uq_permissions_resource_action ON permissions(resource, action);

CREATE TABLE identities (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE,
    user_id BIGINT NOT NULL REFERENCES users(id),
    provider VARCHAR(20) NOT NULL,
    provider_subject VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    display_name VARCHAR(150),
    username VARCHAR(100),
    picture_url VARCHAR(500),
    attributes JSONB NOT NULL DEFAULT '{}'::jsonb,
    linked_at TIMESTAMPTZ NOT NULL,
    last_login_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(255) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(255) NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    deleted_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_identities_provider_subject UNIQUE (provider, provider_subject)
);
CREATE UNIQUE INDEX uq_identities_active_user_provider ON identities (user_id, provider) WHERE is_deleted = FALSE;
