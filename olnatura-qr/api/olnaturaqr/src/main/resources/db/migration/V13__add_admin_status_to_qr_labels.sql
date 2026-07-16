ALTER TABLE qr_labels
  ADD COLUMN IF NOT EXISTS admin_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

UPDATE qr_labels
SET admin_status = 'ACTIVE'
WHERE admin_status IS NULL OR admin_status = '';

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'chk_qr_labels_admin_status'
  ) THEN
    ALTER TABLE qr_labels
      ADD CONSTRAINT chk_qr_labels_admin_status
      CHECK (admin_status IN ('ACTIVE', 'INACTIVE', 'BAJA'));
  END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_qr_labels_admin_status ON qr_labels(admin_status);
