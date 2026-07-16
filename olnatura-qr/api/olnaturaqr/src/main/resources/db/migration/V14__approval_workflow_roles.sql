-- Roles operativos nuevos
INSERT INTO roles(name) VALUES
  ('PRODUCCION'),
  ('CALIDAD')
ON CONFLICT DO NOTHING;

-- Aprobaciones parciales (Empaque Primario: Calidad + Inspección)
ALTER TABLE qr_labels
  ADD COLUMN IF NOT EXISTS calidad_approved_at TIMESTAMPTZ NULL,
  ADD COLUMN IF NOT EXISTS calidad_approved_by UUID NULL REFERENCES users(id) ON DELETE SET NULL,
  ADD COLUMN IF NOT EXISTS inspeccion_approved_at TIMESTAMPTZ NULL,
  ADD COLUMN IF NOT EXISTS inspeccion_approved_by UUID NULL REFERENCES users(id) ON DELETE SET NULL;

-- Estado inicial operativo: CUARENTENA (ya no PENDING / Open de Dynamics)
UPDATE qr_labels
SET status = 'CUARENTENA'
WHERE UPPER(status) IN ('PENDING', 'LIBERADO', 'DESCONOCIDO', 'OPEN')
   OR status IS NULL
   OR TRIM(status) = '';
