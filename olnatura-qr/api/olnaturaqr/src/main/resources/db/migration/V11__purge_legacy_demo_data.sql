-- Purge legacy demo rows that older Flyway seeds (pre-separation) may have inserted.
-- No DDL / no schema or entity changes — DELETE only of known demo identifiers.

DELETE FROM audit_events
WHERE actor_email LIKE '%@demo.local'
   OR lote IN (
        '260112-MES003456',
        'LOTE-TEST-001',
        '251201-MEM0003454',
        'DEMO-2026-001',
        'DEMO-2026-002'
      )
   OR actor_id IN (SELECT id FROM users WHERE email LIKE '%@demo.local');

DELETE FROM scan_events
WHERE device_id IN ('WEB-DEMO-1', 'WEB-DEMO-2', 'ANDROID-DEMO', 'WEB-DEMO')
   OR lote IN (
        '260112-MES003456',
        'LOTE-TEST-001',
        '251201-MEM0003454',
        'DEMO-2026-001',
        'DEMO-2026-002'
      )
   OR scanned_by IN (SELECT id FROM users WHERE email LIKE '%@demo.local');

DELETE FROM qr_labels
WHERE lote IN (
  '260112-MES003456',
  'LOTE-TEST-001',
  '251201-MEM0003454',
  'DEMO-2026-001',
  'DEMO-2026-002'
);

DELETE FROM users
WHERE email LIKE '%@demo.local';
