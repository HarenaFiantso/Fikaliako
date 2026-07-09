-- Fikaliako — authentication & authorization (project book, ch. 4.7, 7.3)
--
-- Adds the 'business' role (premium establishment accounts that manage their
-- own profile), durable refresh-token sessions, OTP codes for phone
-- verification / password reset, and the user ↔ establishment manager link.
--
-- Deviation note: the book (ch. 7.3) places refresh-token revocation and OTP
-- queues in Redis. They are persisted here instead — revocation must survive a
-- Redis flush and the auth flows must be testable without Docker; Redis can
-- later be layered on as a read-through cache without a contract change.

-- Premium establishment accounts (restaurants, gargottes, cafés…). Orthogonal
-- to 'moderator': managing one's own establishment is not moderation power.
ALTER TYPE user_role ADD VALUE 'business' BEFORE 'moderator';

-- Refresh tokens are opaque 256-bit random values handed to the client once;
-- only their SHA-256 hash is stored. Tokens rotate on every /v1/auth/refresh:
-- the presented token is revoked and a successor is issued within the same
-- family. Presenting an already-revoked token is treated as theft and revokes
-- the whole family (rotation reuse detection).
CREATE TABLE refresh_tokens (
    id         UUID PRIMARY KEY,
    user_id    UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash TEXT NOT NULL UNIQUE,
    -- one family per login session; rotation keeps the family id
    family_id  UUID NOT NULL,
    issued_at  TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ
);

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_family ON refresh_tokens (family_id);

-- One-time codes sent by SMS (ch. 4.7): phone verification at signup and
-- password reset. Codes are stored hashed, expire quickly, and allow a bounded
-- number of guesses. Issuance is rate-limited to 5/hour/number (ch. 7.3) by
-- counting recent rows.
CREATE TYPE otp_purpose AS ENUM ('verify_phone', 'reset_password');

CREATE TABLE phone_otps (
    id          UUID PRIMARY KEY,
    phone       TEXT NOT NULL,
    purpose     otp_purpose NOT NULL,
    code_hash   TEXT NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL,
    attempts    SMALLINT NOT NULL DEFAULT 0,
    consumed_at TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_phone_otps_lookup ON phone_otps (phone, purpose, created_at DESC);

-- Which business accounts manage which establishments. Granted by an admin
-- (audit-logged); a business account may manage several establishments and an
-- establishment may have several managers.
CREATE TABLE establishment_managers (
    establishment_id UUID NOT NULL REFERENCES establishments (id) ON DELETE CASCADE,
    user_id          UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    granted_by       UUID REFERENCES users (id) ON DELETE SET NULL,
    created_at       TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (establishment_id, user_id)
);

CREATE INDEX idx_establishment_managers_user ON establishment_managers (user_id);
