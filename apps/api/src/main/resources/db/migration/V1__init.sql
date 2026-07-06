-- Fikaliako — geospatial foundation (project book, ch. 6)
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TYPE establishment_type AS ENUM (
    'restaurant', 'gargotte', 'cafe', 'snack', 'food_truck',
    'street_vendor', 'pastry_shop', 'bar_restaurant', 'hotel_restaurant'
);

CREATE TYPE establishment_status AS ENUM ('active', 'closed', 'pending');

CREATE TABLE establishments (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name              TEXT NOT NULL,
    slug              TEXT NOT NULL UNIQUE,
    type              establishment_type NOT NULL,
    position          GEOGRAPHY(Point, 4326) NOT NULL,
    address           TEXT,
    district          TEXT,
    city              TEXT NOT NULL DEFAULT 'Antananarivo',
    phone             TEXT,
    whatsapp          TEXT,
    facebook_url      TEXT,
    website           TEXT,
    avg_price_ar      INTEGER,
    verified          BOOLEAN NOT NULL DEFAULT FALSE,
    status            establishment_status NOT NULL DEFAULT 'pending',
    -- boolean attributes filterable in a single query (ch. 4.3 / 6.1)
    delivery          BOOLEAN NOT NULL DEFAULT FALSE,
    parking           BOOLEAN NOT NULL DEFAULT FALSE,
    wifi              BOOLEAN NOT NULL DEFAULT FALSE,
    wheelchair_access BOOLEAN NOT NULL DEFAULT FALSE,
    air_conditioning  BOOLEAN NOT NULL DEFAULT FALSE,
    terrace           BOOLEAN NOT NULL DEFAULT FALSE,
    family_friendly   BOOLEAN NOT NULL DEFAULT FALSE,
    romantic          BOOLEAN NOT NULL DEFAULT FALSE,
    student_friendly  BOOLEAN NOT NULL DEFAULT FALSE,
    scenic_view       BOOLEAN NOT NULL DEFAULT FALSE,
    open_24h          BOOLEAN NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_establishments_position ON establishments USING GIST (position);
CREATE INDEX idx_establishments_name_trgm ON establishments USING GIN (name gin_trgm_ops);
CREATE INDEX idx_establishments_status ON establishments (status);

CREATE TABLE opening_hours (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    establishment_id UUID NOT NULL REFERENCES establishments (id) ON DELETE CASCADE,
    day_of_week      SMALLINT NOT NULL CHECK (day_of_week BETWEEN 0 AND 6),
    opens_at         TIME NOT NULL,
    closes_at        TIME NOT NULL
);

CREATE INDEX idx_opening_hours_establishment ON opening_hours (establishment_id);
