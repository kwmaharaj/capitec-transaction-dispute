-- Allow disputes module to build dispute+transaction views without N+1 queries.
-- Read-only access to transactions schema/table.
GRANT USAGE ON SCHEMA transactions TO disputes_app;
GRANT SELECT ON TABLE transactions.transactions TO disputes_app;
