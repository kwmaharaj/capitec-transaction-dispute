-- Rename ambiguous status columns to dispute_status and history columns to explicit names.
ALTER TABLE disputes.disputes RENAME COLUMN status TO dispute_status;

ALTER TABLE disputes.dispute_history RENAME COLUMN from_status TO from_dispute_status;

ALTER TABLE disputes.dispute_history RENAME COLUMN to_status TO to_dispute_status;
