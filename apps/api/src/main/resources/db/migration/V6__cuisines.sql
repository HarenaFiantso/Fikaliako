-- Fikaliako — cuisine referential (project book, ch. 3 "craving", 4.3)
-- Supports the "what am I craving?" discovery axis alongside budget and distance.
-- A referential (not booleans) so the catalogue grows without migrations.

CREATE TABLE cuisines (
    code       TEXT PRIMARY KEY,
    label_fr   TEXT NOT NULL,
    label_mg   TEXT NOT NULL,
    sort_order SMALLINT NOT NULL DEFAULT 0
);

INSERT INTO cuisines (code, label_fr, label_mg, sort_order) VALUES
    ('malagasy', 'Malgache', 'Sakafo gasy', 1),
    ('street_food', 'Cuisine de rue', 'Sakafo an-dalambe', 2),
    ('grill', 'Grillades', 'Atono', 3),
    ('seafood', 'Fruits de mer', 'Hazan-dranomasina', 4),
    ('chinese', 'Chinoise', 'Sinoa', 5),
    ('european', 'Européenne', 'Eoropeana', 6),
    ('fast_food', 'Fast-food', 'Fast-food', 7),
    ('bakery', 'Boulangerie & pâtisserie', 'Mofo sy mofomamy', 8),
    ('vegetarian', 'Végétarienne', 'Tsy misy hena', 9),
    ('drinks', 'Boissons & jus', 'Zava-pisotro', 10);

CREATE TABLE establishment_cuisines (
    establishment_id UUID NOT NULL REFERENCES establishments (id) ON DELETE CASCADE,
    cuisine_code     TEXT NOT NULL REFERENCES cuisines (code),
    PRIMARY KEY (establishment_id, cuisine_code)
);

CREATE INDEX idx_est_cuisines_cuisine ON establishment_cuisines (cuisine_code);
