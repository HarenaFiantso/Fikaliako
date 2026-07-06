-- Fikaliako — establishment lifecycle + budget ordering (project book, ch. 4.1, 6.2, 7.4)
-- Extends the V1 base with an update timestamp and the index backing the
-- "open establishments nearby, cheapest first" core query.

-- Shared trigger to keep updated_at columns current on any UPDATE. Reused by
-- every mutable table below.
CREATE FUNCTION set_updated_at() RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

ALTER TABLE establishments
    ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT now();

CREATE TRIGGER trg_establishments_updated_at
    BEFORE UPDATE ON establishments
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- The core query filters spatially with ST_DWithin (GiST on position) then
-- orders by price. This btree serves the "cheapest first" sort over the small
-- candidate set the spatial filter returns (ch. 3 — budget is the king filter).
CREATE INDEX idx_establishments_avg_price ON establishments (avg_price_ar);
