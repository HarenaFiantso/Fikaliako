-- Fikaliako — reviews, rating aggregates, favorites (project book, ch. 4.4, 4.5, 6.1)

CREATE TYPE review_status AS ENUM ('published', 'hidden', 'flagged');

-- One review per user per establishment (UNIQUE), scored 1..5 on five criteria.
-- global_note is the weighted mean with quality counted twice (weights sum to 6),
-- computed in the database so it can never drift from the criterion scores.
CREATE TABLE reviews (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    establishment_id   UUID NOT NULL REFERENCES establishments (id) ON DELETE CASCADE,
    user_id            UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    rating_quality     SMALLINT NOT NULL CHECK (rating_quality BETWEEN 1 AND 5),
    rating_price       SMALLINT NOT NULL CHECK (rating_price BETWEEN 1 AND 5),
    rating_cleanliness SMALLINT NOT NULL CHECK (rating_cleanliness BETWEEN 1 AND 5),
    rating_speed       SMALLINT NOT NULL CHECK (rating_speed BETWEEN 1 AND 5),
    rating_welcome     SMALLINT NOT NULL CHECK (rating_welcome BETWEEN 1 AND 5),
    global_note        NUMERIC(3, 2) GENERATED ALWAYS AS (
        (rating_quality * 2 + rating_price + rating_cleanliness + rating_speed + rating_welcome)::NUMERIC / 6
    ) STORED,
    comment            TEXT,
    status             review_status NOT NULL DEFAULT 'published',
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (establishment_id, user_id)
);

CREATE INDEX idx_reviews_establishment ON reviews (establishment_id);
CREATE INDEX idx_reviews_user ON reviews (user_id);

CREATE TRIGGER trg_reviews_updated_at
    BEFORE UPDATE ON reviews
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- Denormalised rating snapshot per establishment, recomputed nightly (ch. 4.5).
-- bayesian_note dampens establishments with few reviews for fair rankings; it is
-- the ordering key for "best rated" lists. Kept off the establishments row so the
-- nightly ranking job never contends with the establishment write path.
CREATE TABLE establishment_ratings (
    establishment_id UUID PRIMARY KEY REFERENCES establishments (id) ON DELETE CASCADE,
    review_count     INTEGER NOT NULL DEFAULT 0,
    avg_quality      NUMERIC(3, 2),
    avg_price        NUMERIC(3, 2),
    avg_cleanliness  NUMERIC(3, 2),
    avg_speed        NUMERIC(3, 2),
    avg_welcome      NUMERIC(3, 2),
    avg_global       NUMERIC(3, 2),
    bayesian_note    NUMERIC(3, 2),
    computed_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_establishment_ratings_bayesian ON establishment_ratings (bayesian_note DESC);

-- Favorites: N–N user ↔ establishment (ch. 4.6). PUT/DELETE toggle the row.
CREATE TABLE favorites (
    user_id          UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    establishment_id UUID NOT NULL REFERENCES establishments (id) ON DELETE CASCADE,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, establishment_id)
);

CREATE INDEX idx_favorites_establishment ON favorites (establishment_id);
