-- Fikaliako — accounts module (project book, ch. 7.3)
-- Phone-number accounts. OTP verification, refresh-token revocation and
-- per-number rate limiting live in Redis, not here; this table holds only the
-- durable identity. Argon2id password hashes are stored opaque in password_hash.

CREATE TYPE user_role AS ENUM ('user', 'moderator', 'admin');

CREATE TYPE account_status AS ENUM ('active', 'suspended', 'deleted');

CREATE TABLE users (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    -- E.164 phone number; the sole login identifier (ch. 4.7, no email at MVP)
    phone          TEXT NOT NULL UNIQUE,
    phone_verified BOOLEAN NOT NULL DEFAULT FALSE,
    display_name   TEXT NOT NULL,
    password_hash  TEXT NOT NULL,
    role           user_role NOT NULL DEFAULT 'user',
    status         account_status NOT NULL DEFAULT 'active',
    -- MVP ships FR + MG only (ch. 5.2); UI language preference
    locale         TEXT NOT NULL DEFAULT 'fr' CHECK (locale IN ('fr', 'mg')),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
