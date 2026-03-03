CREATE TABLE IF NOT EXISTS disputes.disputes (
    dispute_id     UUID PRIMARY KEY,
    transaction_id UUID NOT NULL,
    user_id        UUID NOT NULL,
    reason_code    VARCHAR(64) NOT NULL,
    note           TEXT NOT NULL,
    status         VARCHAR(32) NOT NULL,
    version        BIGINT NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ NOT NULL,
    updated_at     TIMESTAMPTZ NOT NULL,
    decided_at     TIMESTAMPTZ NULL,
    decided_by     UUID NULL
);

-- Prevent duplicates: one dispute per transaction per user.
CREATE UNIQUE INDEX IF NOT EXISTS ux_disputes_tx_user
    ON disputes.disputes(transaction_id, user_id);

CREATE INDEX IF NOT EXISTS idx_disputes_user_id ON disputes.disputes(user_id);
CREATE INDEX IF NOT EXISTS idx_disputes_status ON disputes.disputes(status);
CREATE INDEX IF NOT EXISTS idx_disputes_transaction_id ON disputes.disputes(transaction_id);
CREATE INDEX IF NOT EXISTS idx_disputes_user_status ON disputes.disputes(user_id, status);
