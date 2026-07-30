-- V16: desacoplar lote_comments de qr_labels (compatible hacia atrás).
-- Ver docs/comments-lote-decoupling.md
--
-- - Elimina únicamente la FK a qr_labels (sin DROP TABLE ni cambio de columnas).
-- - Conserva todos los comentarios existentes.
-- - Asegura el índice por lote (ya creado en V15; IF NOT EXISTS por seguridad).

DO $$
DECLARE
  fk_name text;
BEGIN
  SELECT c.conname INTO fk_name
  FROM pg_constraint c
  WHERE c.conrelid = 'lote_comments'::regclass
    AND c.contype = 'f'
    AND c.confrelid = 'qr_labels'::regclass
  LIMIT 1;

  IF fk_name IS NOT NULL THEN
    EXECUTE format('ALTER TABLE lote_comments DROP CONSTRAINT %I', fk_name);
  END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_lote_comments_lote_created
  ON lote_comments (lote, created_at ASC);
