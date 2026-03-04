-- Demo app users + sample data for UI
-- Postgres credentials (POSTGRES_*) are for the datasource; app users live in users table.
-- Passwords hashed with bcrypt (same algorithm as Spring BCryptPasswordEncoder).

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- 1) Demo app users (admin, inp, alm)
INSERT INTO users (username, email, password_hash, enabled, role_id, created_at)
SELECT 'admin', 'admin@demo.local', crypt('Admin123!', gen_salt('bf')), true, r.id, now()
FROM roles r WHERE r.name = 'ADMIN'
ON CONFLICT (username) DO NOTHING;

INSERT INTO users (username, email, password_hash, enabled, role_id, created_at)
SELECT 'inp', 'inp@demo.local', crypt('Inp123!', gen_salt('bf')), true, r.id, now()
FROM roles r WHERE r.name = 'INSPECCION'
ON CONFLICT (username) DO NOTHING;

INSERT INTO users (username, email, password_hash, enabled, role_id, created_at)
SELECT 'alm', 'alm@demo.local', crypt('Alm123!', gen_salt('bf')), true, r.id, now()
FROM roles r WHERE r.name = 'ALMACEN'
ON CONFLICT (username) DO NOTHING;

-- 2) Extra qr_labels for demo (V3/V4 already add some)
INSERT INTO qr_labels (
  tipo_material, nombre, codigo, lote,
  fecha_entrada, caducidad, reanalisis,
  envase_num, envase_total, status_dinamico
) VALUES
  ('MP', 'VITAMINA C', 'VIT-001', 'DEMO-2026-001',
   '2026-01-10', '2027-06-30', NULL, 5, 20, 'LIBERADO'),
  ('INSUMO', 'LACTOSA', 'LAC-002', 'DEMO-2026-002',
   '2026-01-15', NULL, '2027-03-01', 1, 10, 'PENDING')
ON CONFLICT (lote) DO NOTHING;

-- 3) scan_events (requires existing qr_labels and users)
-- scan_events has no UNIQUE; Flyway runs each migration once, so no duplicates.
INSERT INTO scan_events (lote, scanned_by, device_id, created_at)
SELECT '251201-MEM0003454', u.id, 'WEB-DEMO-1', now() - interval '2 hours'
FROM users u WHERE u.username = 'admin' LIMIT 1;

INSERT INTO scan_events (lote, scanned_by, device_id, created_at)
SELECT 'LOTE-TEST-001', u.id, 'WEB-DEMO-2', now() - interval '1 hour'
FROM users u WHERE u.username = 'inp' LIMIT 1;

INSERT INTO scan_events (lote, scanned_by, device_id, created_at)
SELECT '260112-MES003456', u.id, 'ANDROID-DEMO', now() - interval '30 minutes'
FROM users u WHERE u.username = 'alm' LIMIT 1;

-- 4) audit_events
INSERT INTO audit_events (actor_id, actor_email, actor_rol, action_type, lote, metadata, device_id)
SELECT u.id, u.email, 'ADMIN', 'CHANGE_STATUS', 'LOTE-TEST-001', '{"status":"LIBERADO"}'::jsonb, 'WEB-DEMO'
FROM users u WHERE u.username = 'admin' LIMIT 1;

INSERT INTO audit_events (actor_id, actor_email, actor_rol, action_type, lote, metadata, device_id)
SELECT u.id, u.email, 'INSPECCION', 'SCAN', '251201-MEM0003454', '{}'::jsonb, 'WEB-DEMO'
FROM users u WHERE u.username = 'inp' LIMIT 1;

INSERT INTO audit_events (actor_id, actor_email, actor_rol, action_type, lote, metadata, device_id)
SELECT u.id, u.email, 'ALMACEN', 'GENERATE_LABEL', 'DEMO-2026-001', '{}'::jsonb, NULL
FROM users u WHERE u.username = 'alm' LIMIT 1;