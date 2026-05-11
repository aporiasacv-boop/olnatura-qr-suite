CREATE TABLE audit_events (
  id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
  actor_id      UUID NULL REFERENCES users(id) ON DELETE SET NULL,
  actor_email   VARCHAR(150) NULL,
  actor_rol     VARCHAR(50) NULL,
  action_type   VARCHAR(80) NOT NULL,
  lote          VARCHAR(120) NULL,
  metadata      JSONB NULL,
  device_id     VARCHAR(120) NULL
);

CREATE INDEX idx_audit_events_created ON audit_events(created_at DESC);
CREATE INDEX idx_audit_events_action ON audit_events(action_type);
CREATE INDEX idx_audit_events_lote ON audit_events(lote);
