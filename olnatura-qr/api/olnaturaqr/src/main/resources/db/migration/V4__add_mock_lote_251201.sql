-- Mock completo para demo: 251201-MEM0003454
INSERT INTO qr_labels (
  tipo_material, nombre, codigo, lote,
  fecha_entrada, caducidad, reanalisis,
  envase_num, envase_total, status_dinamico
) VALUES (
  'MP', 'SORBITOL', '91290129109', '251201-MEM0003454',
  '2025-12-01', '2027-12-01', NULL, 1, 20, 'DESCONOCIDO'
) ON CONFLICT (lote) DO NOTHING;
