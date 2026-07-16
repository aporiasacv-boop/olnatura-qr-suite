-- Demo data ONLY (perfil spring:dev → spring.flyway.locations includes classpath:db/demo).
-- Never loaded in production.

-- Users demo (sin admin: el admin inicial lo crea AdminBootstrapRunner en arranque).
INSERT INTO users (username, email, password_hash, enabled, role_id, created_at)
SELECT 'inp', 'inp@demo.local', crypt('Inp123!', gen_salt('bf')), true, r.id, now()
FROM roles r WHERE r.name = 'INSPECCION'
ON CONFLICT (username) DO NOTHING;

INSERT INTO users (username, email, password_hash, enabled, role_id, created_at)
SELECT 'alm', 'alm@demo.local', crypt('Alm123!', gen_salt('bf')), true, r.id, now()
FROM roles r WHERE r.name = 'ALMACEN'
ON CONFLICT (username) DO NOTHING;

-- Labels demo (column status after V10; on fresh dev Flyway applies demo after all migrations)
INSERT INTO qr_labels (
  tipo_material, nombre, codigo, lote,
  fecha_entrada, caducidad, reanalisis,
  envase_num, envase_total, status
) VALUES
  ('INSUMO', 'Sorbitol', '563321099', '260112-MES003456',
   '2025-01-15', '2026-06-30', NULL, 1, 1, 'DESCONOCIDO'),
  ('INSUMO', 'Ejemplo', '000000001', 'LOTE-TEST-001',
   '2025-02-01', '2026-12-31', NULL, 1, 1, 'DESCONOCIDO'),
  ('MP', 'SORBITOL', '91290129109', '251201-MEM0003454',
   '2025-12-01', '2027-12-01', NULL, 1, 20, 'DESCONOCIDO'),
  ('MP', 'VITAMINA C', 'VIT-001', 'DEMO-2026-001',
   '2026-01-10', '2027-06-30', NULL, 5, 20, 'LIBERADO'),
  ('INSUMO', 'LACTOSA', 'LAC-002', 'DEMO-2026-002',
   '2026-01-15', NULL, '2027-03-01', 1, 10, 'PENDING')
ON CONFLICT (lote) DO NOTHING;

INSERT INTO scan_events (lote, scanned_by, device_id, created_at)
SELECT '251201-MEM0003454', u.id, 'WEB-DEMO-1', now() - interval '2 hours'
FROM users u WHERE u.username = 'alm' AND u.email = 'alm@demo.local' LIMIT 1;

INSERT INTO scan_events (lote, scanned_by, device_id, created_at)
SELECT 'LOTE-TEST-001', u.id, 'WEB-DEMO-2', now() - interval '1 hour'
FROM users u WHERE u.username = 'inp' AND u.email = 'inp@demo.local' LIMIT 1;

INSERT INTO scan_events (lote, scanned_by, device_id, created_at)
SELECT '260112-MES003456', u.id, 'ANDROID-DEMO', now() - interval '30 minutes'
FROM users u WHERE u.username = 'alm' AND u.email = 'alm@demo.local' LIMIT 1;

INSERT INTO audit_events (actor_id, actor_email, actor_rol, action_type, lote, metadata, device_id)
SELECT u.id, u.email, 'ALMACEN', 'CHANGE_STATUS', 'LOTE-TEST-001', '{"status":"LIBERADO"}'::jsonb, 'WEB-DEMO'
FROM users u WHERE u.username = 'alm' AND u.email = 'alm@demo.local' LIMIT 1;

INSERT INTO audit_events (actor_id, actor_email, actor_rol, action_type, lote, metadata, device_id)
SELECT u.id, u.email, 'INSPECCION', 'SCAN', '251201-MEM0003454', '{}'::jsonb, 'WEB-DEMO'
FROM users u WHERE u.username = 'inp' AND u.email = 'inp@demo.local' LIMIT 1;

INSERT INTO audit_events (actor_id, actor_email, actor_rol, action_type, lote, metadata, device_id)
SELECT u.id, u.email, 'ALMACEN', 'GENERATE_LABEL', 'DEMO-2026-001', '{}'::jsonb, NULL
FROM users u WHERE u.username = 'alm' AND u.email = 'alm@demo.local' LIMIT 1;
