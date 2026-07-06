-- Fikaliako — payment methods referential (project book, ch. 4.3, 6.1)
-- Dedicated N–N referential rather than boolean columns: the "mobile payment"
-- filter resolves to "any row with is_mobile_money = true", so operators can be
-- added without a schema change.

CREATE TABLE payment_methods (
    code            TEXT PRIMARY KEY,
    label_fr        TEXT NOT NULL,
    label_mg        TEXT NOT NULL,
    is_mobile_money BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order      SMALLINT NOT NULL DEFAULT 0
);

INSERT INTO payment_methods (code, label_fr, label_mg, is_mobile_money, sort_order) VALUES
    ('cash', 'Espèces', 'Vola madinika', FALSE, 1),
    ('mvola', 'MVola', 'MVola', TRUE, 2),
    ('orange_money', 'Orange Money', 'Orange Money', TRUE, 3),
    ('airtel_money', 'Airtel Money', 'Airtel Money', TRUE, 4),
    ('carte', 'Carte bancaire', 'Karatra', FALSE, 5);

CREATE TABLE establishment_payment_methods (
    establishment_id    UUID NOT NULL REFERENCES establishments (id) ON DELETE CASCADE,
    payment_method_code TEXT NOT NULL REFERENCES payment_methods (code),
    PRIMARY KEY (establishment_id, payment_method_code)
);

CREATE INDEX idx_est_payment_methods_code ON establishment_payment_methods (payment_method_code);
