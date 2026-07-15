-- Ensure PostgreSQL extensions required by the schema remain available.
-- Idempotent and compatible with Azure Database for PostgreSQL Flexible Server
-- (extensions must be allowed on the server; CREATE EXTENSION IF NOT EXISTS is safe).
-- Does not alter existing schema objects or change uuid defaults.

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS pgcrypto;
