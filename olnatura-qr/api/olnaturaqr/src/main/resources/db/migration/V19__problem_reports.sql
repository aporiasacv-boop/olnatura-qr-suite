CREATE TABLE problem_reports (
  id                     UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  kind                   VARCHAR(20) NOT NULL,
  lote                   VARCHAR(120),
  reason                 VARCHAR(200) NOT NULL,
  comment_body           TEXT,
  reporter_user_id       UUID,
  reporter_username      VARCHAR(100),
  status                 VARCHAR(20) NOT NULL DEFAULT 'OPEN',
  created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
  resolved_at            TIMESTAMPTZ,
  resolved_by_username   VARCHAR(100),
  CONSTRAINT problem_reports_kind_chk CHECK (kind IN ('SCAN', 'ACCESS')),
  CONSTRAINT problem_reports_status_chk CHECK (status IN ('OPEN', 'RESOLVED')),
  CONSTRAINT problem_reports_reason_not_blank CHECK (char_length(trim(reason)) > 0)
);

CREATE INDEX idx_problem_reports_status_created ON problem_reports (status, created_at DESC);
CREATE INDEX idx_problem_reports_created ON problem_reports (created_at DESC);
