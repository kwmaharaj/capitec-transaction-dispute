CREATE TABLE IF NOT EXISTS disputes.dispute_history (
    history_id      UUID PRIMARY KEY,
    dispute_id      UUID NOT NULL,
    from_status     VARCHAR(32) NULL,
    to_status       VARCHAR(32) NOT NULL,
    note            TEXT NOT NULL,
    actor_role      VARCHAR(16) NOT NULL,
    actor_user_id   UUID NOT NULL,
    changed_at      TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_dispute_history_dispute_id ON disputes.dispute_history(dispute_id);
CREATE INDEX IF NOT EXISTS idx_dispute_history_changed_at ON disputes.dispute_history(changed_at);
