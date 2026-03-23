import * as React from "react";
import {
  Button,
  Dropdown,
  Input,
  Option,
  makeStyles,
  shorthands,
} from "@fluentui/react-components";
import { useNavigate } from "react-router-dom";
import { brand } from "../styles/brand";
import { api, ApiError } from "../api/client";

const useStyles = makeStyles({
  root: {
    minHeight: "100vh",
    display: "grid",
    placeItems: "center",
    backgroundColor: brand.background,
    ...shorthands.padding("24px"),
  },
  card: {
    width: "440px",
    maxWidth: "100%",
    backgroundColor: brand.surface,
    borderRadius: "12px",
    boxShadow: "0 2px 8px rgba(0,0,0,0.06)",
    ...shorthands.border("1px", "solid", brand.border),
    ...shorthands.padding("24px"),
  },
  header: {
    marginBottom: "20px",
  },
  title: {
    fontSize: "20px",
    fontWeight: 600,
    color: brand.text,
  },
  subtitle: {
    fontSize: "14px",
    color: brand.muted,
    marginTop: "4px",
  },
  form: {
    display: "grid",
    rowGap: "16px",
  },
  row: {
    display: "grid",
    rowGap: "8px",
  },
  label: {
    fontSize: "14px",
    fontWeight: 500,
    color: brand.text2,
  },
  input: { width: "100%", minWidth: 0 },
  actions: {
    display: "grid",
    rowGap: "10px",
    marginTop: "8px",
  },
  success: {
    color: brand.successFg,
    backgroundColor: brand.successBg,
    ...shorthands.padding("12px"),
    borderRadius: "8px",
    fontSize: "14px",
  },
  error: {
    color: brand.dangerFg,
    fontSize: "13px",
  },
});

type RequestUserPayload = {
  username: string;
  email: string;
  roleRequested: "ALMACEN" | "INSPECCION";
  password: string;
};

export default function RegisterRequestPage() {
  const s = useStyles();
  const nav = useNavigate();

  const [nombre, setNombre] = React.useState("");
  const [correo, setCorreo] = React.useState("");
  const [area, setArea] = React.useState<"ALMACEN" | "INSPECCION" | "">("");
  const [password, setPassword] = React.useState("");
  const [busy, setBusy] = React.useState(false);
  const [error, setError] = React.useState<string | null>(null);
  const [success, setSuccess] = React.useState(false);

  const canSubmit =
    nombre.trim().length > 0 &&
    correo.trim().length > 0 &&
    area !== "" &&
    password.trim().length > 0 &&
    !busy;

  const onSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setBusy(true);
    setError(null);
    setSuccess(false);

    try {
      if (area === "") {
        setError("Selecciona un área.");
        setBusy(false);
        return;
      }

      const payload: RequestUserPayload = {
        username: nombre.trim(),
        email: correo.trim(),
        roleRequested: area,
        password: password.trim(),
      };

      await api("/auth/request-access", {
        method: "POST",
        body: payload,
        toast: false,
      });

      setSuccess(true);
      setNombre("");
      setCorreo("");
      setArea("");
      setPassword("");
    } catch (err) {
      const ae = err as ApiError;
      setError(ae?.message || "No se pudo enviar la solicitud.");
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className={s.root}>
      <div className={s.card}>
        <div className={s.header}>
          <div className={s.title}>Crear usuario</div>
          <div className={s.subtitle}>Solicitud de alta para acceso al sistema</div>
        </div>

        <form onSubmit={onSubmit} className={s.form}>
          <div className={s.row}>
            <span className={s.label}>Usuario</span>
            <Input
              appearance="outline"
              size="large"
              className={s.input}
              value={nombre}
              onChange={(_, d) => setNombre(d.value)}
              placeholder="Ingresa tu usuario"
            />
          </div>

          <div className={s.row}>
            <span className={s.label}>Correo</span>
            <Input
              appearance="outline"
              size="large"
              className={s.input}
              value={correo}
              onChange={(_, d) => setCorreo(d.value)}
              placeholder="correo@ejemplo.com"
              type="email"
            />
          </div>

          <div className={s.row}>
            <span className={s.label}>Área</span>
            <Dropdown
              appearance="outline"
              size="large"
              placeholder="Selecciona un área"
              selectedOptions={area ? [area] : []}
              onOptionSelect={(_, data) =>
                setArea((data.optionValue ?? "") as "ALMACEN" | "INSPECCION" | "")
              }
            >
              <Option value="ALMACEN">ALMACÉN</Option>
              <Option value="INSPECCION">INSPECCIÓN</Option>
            </Dropdown>
          </div>

          <div className={s.row}>
            <span className={s.label}>Contraseña</span>
            <Input
              appearance="outline"
              size="large"
              className={s.input}
              value={password}
              onChange={(_, d) => setPassword(d.value)}
              placeholder="Ingresa una contraseña"
              type="password"
            />
          </div>

          {success ? (
            <div className={s.success}>Solicitud enviada, el administrador la revisará</div>
          ) : null}
          {error ? <div className={s.error}>{error}</div> : null}

          <div className={s.actions}>
            <Button appearance="primary" size="large" type="submit" disabled={!canSubmit}>
              {busy ? "Enviando..." : "Enviar solicitud"}
            </Button>
            <Button appearance="secondary" size="large" type="button" onClick={() => nav("/login")}>
              Volver a inicio de sesión
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}
