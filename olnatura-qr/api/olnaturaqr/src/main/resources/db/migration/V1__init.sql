-- Extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Roles base (admin / almacen / inspeccion)
CREATE TABLE roles (
  id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  name        VARCHAR(50) NOT NULL UNIQUE
);

INSERT INTO roles(name) VALUES
  ('ADMIN'),
  ('ALMACEN'),
  ('INSPECCION')
ON CONFLICT DO NOTHING;

-- Usuarios del sistema (solo app autorizada después)
CREATE TABLE users (
  id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  username      VARCHAR(80) NOT NULL UNIQUE,
  email         VARCHAR(150) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  enabled       BOOLEAN NOT NULL DEFAULT FALSE,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Un usuario puede tener varios roles
CREATE TABLE user_roles (
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  role_id UUID NOT NULL REFERENCES roles(id) ON DELETE RESTRICT,
  PRIMARY KEY (user_id, role_id)
);

-- QRs (insumos) - llave real: LOTE (irrepetible)
CREATE TABLE qr_labels (
  id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  tipo_material   VARCHAR(60) NOT NULL, -- MEDICAMENTO / INSUMO / OTRO
  nombre          VARCHAR(200) NOT NULL,
  codigo          VARCHAR(80) NOT NULL,
  lote            VARCHAR(120) NOT NULL UNIQUE,
  fecha_entrada   DATE NOT NULL,
  caducidad       DATE NULL,
  reanalisis      DATE NULL,
  envase_num      INT NOT NULL,
  envase_total    INT NOT NULL,
  status_dinamico VARCHAR(40) NOT NULL DEFAULT 'DESCONOCIDO', -- aprobado/rechazado/cuarentena
  created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Registro de escaneos ( auditoría / trazabilidad)
CREATE TABLE scan_events (
  id         UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  lote       VARCHAR(120) NOT NULL REFERENCES qr_labels(lote) ON DELETE CASCADE,
  scanned_by UUID NULL REFERENCES users(id) ON DELETE SET NULL,
  device_id  VARCHAR(120) NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_scan_events_lote ON scan_events(lote);
CREATE INDEX idx_users_enabled ON users(enabled);