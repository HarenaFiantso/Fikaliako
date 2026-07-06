-- Fikaliako — establishment photos (project book, ch. 4.2, 7.3)
-- Binaries live in MinIO (S3-compatible); this table holds only the object key
-- and moderation metadata. Uploads are magic-byte checked and re-encoded before
-- a row lands here, then moderated before becoming publicly visible.

CREATE TYPE photo_status AS ENUM ('pending', 'approved', 'rejected');

CREATE TABLE photos (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    establishment_id UUID NOT NULL REFERENCES establishments (id) ON DELETE CASCADE,
    -- MinIO object key; the binary itself is never stored in Postgres
    storage_key      TEXT NOT NULL UNIQUE,
    uploaded_by      UUID REFERENCES users (id) ON DELETE SET NULL,
    caption          TEXT,
    status           photo_status NOT NULL DEFAULT 'pending',
    is_primary       BOOLEAN NOT NULL DEFAULT FALSE,
    width            INTEGER,
    height           INTEGER,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_photos_establishment ON photos (establishment_id);

-- At most one primary photo per establishment (the card/hero image).
CREATE UNIQUE INDEX idx_photos_one_primary ON photos (establishment_id) WHERE is_primary;
