CREATE TABLE IF NOT EXISTS transactions.transactions (
    transaction_id UUID PRIMARY KEY,
    user_id        UUID NOT NULL,
    posted_at      TIMESTAMPTZ NOT NULL,
    amount         NUMERIC(19, 4) NOT NULL,
    currency       VARCHAR(3) NOT NULL,
    merchant       TEXT NOT NULL,
    status         VARCHAR(32) NOT NULL,
    bin            VARCHAR(12) NOT NULL,
    last4digits    VARCHAR(4) NOT NULL,
    pan_hash       TEXT NOT NULL,
    rrn            TEXT NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_transactions_user_id ON transactions.transactions(user_id);
CREATE INDEX IF NOT EXISTS idx_transactions_posted_at ON transactions.transactions(posted_at);
