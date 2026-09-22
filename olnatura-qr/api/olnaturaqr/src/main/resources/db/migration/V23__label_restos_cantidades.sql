ALTER TABLE qr_labels
  ADD COLUMN IF NOT EXISTS restos_cantidades VARCHAR(500) NULL;

UPDATE qr_labels
SET restos_cantidades = CONCAT('["', REPLACE(TRIM(cantidad_resto), '"', '\"'), '"]')
WHERE restos_enabled = TRUE
  AND cantidad_resto IS NOT NULL
  AND TRIM(cantidad_resto) <> ''
  AND (restos_cantidades IS NULL OR TRIM(restos_cantidades) = '');
