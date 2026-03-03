CREATE TABLE IF NOT EXISTS security.users (
    user_id    UUID PRIMARY KEY,
    username   TEXT NOT NULL UNIQUE,
    password_hash   TEXT NOT NULL,
    roles      TEXT[] NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_security_users_username ON security.users(username);
