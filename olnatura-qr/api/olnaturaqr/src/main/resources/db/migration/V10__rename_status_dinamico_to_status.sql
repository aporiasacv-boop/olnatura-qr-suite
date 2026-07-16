-- Platform QR status (managed only by this app). Renames legacy status_dinamico.
ALTER TABLE qr_labels RENAME COLUMN status_dinamico TO status;
