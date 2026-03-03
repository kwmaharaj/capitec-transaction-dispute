-- Roles
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'security_app') THEN
    CREATE ROLE security_app LOGIN PASSWORD 'security_app_pw';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'transactions_app') THEN
    CREATE ROLE transactions_app LOGIN PASSWORD 'transactions_app_pw';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'disputes_app') THEN
    CREATE ROLE disputes_app LOGIN PASSWORD 'disputes_app_pw';
  END IF;
END$$;

-- Allow connect
GRANT CONNECT ON DATABASE txdispute TO security_app, transactions_app, disputes_app;

-- Schemas owned by their module roles
CREATE SCHEMA IF NOT EXISTS security AUTHORIZATION security_app;
CREATE SCHEMA IF NOT EXISTS transactions AUTHORIZATION transactions_app;
CREATE SCHEMA IF NOT EXISTS disputes AUTHORIZATION disputes_app;

-- Privileges within each schema
GRANT USAGE, CREATE ON SCHEMA security TO security_app;
GRANT USAGE, CREATE ON SCHEMA transactions TO transactions_app;
GRANT USAGE, CREATE ON SCHEMA disputes TO disputes_app;

-- Make future tables/sequences usable by the owning role (safe defaults)
ALTER DEFAULT PRIVILEGES IN SCHEMA security
  GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO security_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA security
  GRANT USAGE, SELECT, UPDATE ON SEQUENCES TO security_app;

ALTER DEFAULT PRIVILEGES IN SCHEMA transactions
  GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO transactions_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA transactions
  GRANT USAGE, SELECT, UPDATE ON SEQUENCES TO transactions_app;

ALTER DEFAULT PRIVILEGES IN SCHEMA disputes
  GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO disputes_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA disputes
  GRANT USAGE, SELECT, UPDATE ON SEQUENCES TO disputes_app;
