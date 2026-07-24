-- Fikaliako — moderator feedback on resolved contributions (project book ch. 4.8)
-- When the back-office rejects a contribution it can record why; the note is
-- shown alongside the resolved queue entry (and, later, to the author).

ALTER TABLE contributions
    ADD COLUMN resolution_note TEXT;
