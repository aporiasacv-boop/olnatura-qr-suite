-- Production-safe: only ensure pgcrypto (needed by some Postgres helper functions).
-- Demo users/labels/scans/audit MOVING to classpath:db/demo (perfil dev only).
CREATE EXTENSION IF NOT EXISTS pgcrypto;
