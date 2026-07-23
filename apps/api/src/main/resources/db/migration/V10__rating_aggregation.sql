-- Fikaliako — discovery ranking signals (project book, ch. 4.2, 4.6)
-- recent_review_count backs the smart-search discovery score ("nombre d'avis
-- récents", ch. 4.2). Refreshed by the nightly rating aggregation job together
-- with the averages and the Bayesian note.

ALTER TABLE establishment_ratings
    ADD COLUMN recent_review_count INTEGER NOT NULL DEFAULT 0;
