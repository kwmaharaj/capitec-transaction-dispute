-- Rename ambiguous status columns to transaction_status for clarity.
ALTER TABLE transactions.transactions RENAME COLUMN status TO transaction_status;
