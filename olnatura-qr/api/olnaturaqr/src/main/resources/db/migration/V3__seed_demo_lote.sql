-- Lotes de demostración para MVP
INSERT INTO qr_labels (
  tipo_material, nombre, codigo, lote,
  fecha_entrada, caducidad, reanalisis,
  envase_num, envase_total, status_dinamico
) VALUES
  ('INSUMO', 'Sorbitol', '563321099', '260112-MES003456',
   '2025-01-15', '2026-06-30', NULL, 1, 1, 'DESCONOCIDO'),
  ('INSUMO', 'Ejemplo', '000000001', 'LOTE-TEST-001',
   '2025-02-01', '2026-12-31', NULL, 1, 1, 'DESCONOCIDO')
ON CONFLICT (lote) DO NOTHING;
