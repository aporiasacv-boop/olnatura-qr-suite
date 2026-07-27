-- Bitácora operativa de comentarios por lote (inmutable: sin update/delete).
CREATE TABLE lote_comments (
  id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  lote                VARCHAR(120) NOT NULL REFERENCES qr_labels(lote) ON DELETE CASCADE,
  author_user_id      UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
  author_username     VARCHAR(100) NOT NULL,
  author_display_name VARCHAR(200) NOT NULL,
  author_role         VARCHAR(50) NOT NULL,
  body                TEXT NOT NULL,
  created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT lote_comments_body_not_blank CHECK (char_length(trim(body)) > 0)
);

CREATE INDEX idx_lote_comments_lote_created ON lote_comments (lote, created_at ASC);
