-- Add public_token for canonical QR format OLNQR:1:<token>
ALTER TABLE qr_labels
ADD COLUMN public_token VARCHAR(64);

-- Backfill: use id without dashes as token for existing rows
UPDATE qr_labels
SET public_token = replace(id::text, '-', '')
WHERE public_token IS NULL;

ALTER TABLE qr_labels
ALTER COLUMN public_token SET NOT NULL;

CREATE UNIQUE INDEX idx_qr_labels_public_token ON qr_labels(public_token);
