-- Fikaliako — community contribution loop + moderation (project book, ch. 4.8, 7.3)
-- Anyone connected can propose a new establishment or correct an existing one.
-- A correction auto-applies once it collects two concordant confirmations,
-- otherwise it waits in the moderation queue. All admin/moderator actions are
-- recorded in an append-only audit log.

CREATE TYPE contribution_type AS ENUM ('create', 'edit', 'correction', 'closure_report');

CREATE TYPE contribution_status AS ENUM ('pending', 'applied', 'rejected', 'superseded');

CREATE TABLE contributions (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    -- NULL establishment_id ⇒ proposal for a brand-new establishment (type = create)
    establishment_id    UUID REFERENCES establishments (id) ON DELETE CASCADE,
    -- author kept as history; SET NULL if the account is later purged (ch. 9)
    author_id           UUID REFERENCES users (id) ON DELETE SET NULL,
    type                contribution_type NOT NULL,
    -- proposed field values / diff as submitted; applied to the row on approval
    payload             JSONB NOT NULL,
    status              contribution_status NOT NULL DEFAULT 'pending',
    -- count of concordant confirmations; auto-applies at 2 (ch. 4.8)
    confirmations_count SMALLINT NOT NULL DEFAULT 0,
    resolved_by         UUID REFERENCES users (id) ON DELETE SET NULL,
    resolved_at         TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_contributions_status ON contributions (status);
CREATE INDEX idx_contributions_establishment ON contributions (establishment_id);

-- One confirmation per user per contribution; agrees = concordant vote.
CREATE TABLE contribution_confirmations (
    contribution_id UUID NOT NULL REFERENCES contributions (id) ON DELETE CASCADE,
    user_id         UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    agrees          BOOLEAN NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (contribution_id, user_id)
);

-- Append-only audit trail for moderation/admin actions (ch. 7.3). No updated_at
-- and no trigger: rows are immutable by contract; UPDATE/DELETE are not issued.
CREATE TABLE audit_log (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    actor_id    UUID REFERENCES users (id) ON DELETE SET NULL,
    action      TEXT NOT NULL,
    entity_type TEXT NOT NULL,
    entity_id   UUID,
    details     JSONB,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_log_entity ON audit_log (entity_type, entity_id);
CREATE INDEX idx_audit_log_actor ON audit_log (actor_id);
