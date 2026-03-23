import * as React from "react";
import {
  Button,
  Card,
  CardHeader,
  CardPreview,
  Input,
  Text,
  makeStyles,
  shorthands,
} from "@fluentui/react-components";
import { useNavigate } from "react-router-dom";
import { brand } from "../styles/brand";

const useStyles = makeStyles({
  root: {
    minHeight: "100vh",
    display: "grid",
    placeItems: "center",
    backgroundColor: brand.background,
    ...shorthands.padding("24px"),
  },
  card: {
    width: "460px",
    maxWidth: "100%",
    ...shorthands.border("1px", "solid", brand.border),
    ...shorthands.borderRadius("14px"),
  },
  inner: {
    display: "grid",
    rowGap: "14px",
    ...shorthands.padding("20px"),
  },
  form: {
    display: "grid",
    rowGap: "12px",
  },
  row: {
    display: "grid",
    rowGap: "8px",
  },
  input: {
    width: "100%",
    minWidth: 0,
  },
  actions: {
    display: "grid",
    rowGap: "10px",
    ...shorthands.margin("4px", "0", "0"),
  },
  success: {
    color: brand.successFg,
    backgroundColor: brand.successBg,
    ...shorthands.padding("8px", "10px"),
    ...shorthands.borderRadius("8px"),
    fontSize: "13px",
  },
  error: {
    color: brand.dangerFg,
    fontSize: "13px",
  },
});

type RequestUserPayload = {
  nombre: string;
  correo: string;
  area: string;
  password: string;
};

export default function RegisterRequestPage() {
  const s = useStyles();
  const nav = useNavigate();

  const [nombre, setNombre] = React.useState("");
  const [correo, setCorreo] = React.useState("");
  const [area, setArea] = React.useState("");
  const [password, setPassword] = React.useState("");
  const [busy, setBusy] = React.useState(false);
  const [error, setError] = React.useState<string | null>(null);
  const [success, setSuccess] = React.useState(false);

  const canSubmit =
    nombre.trim().length > 0 &&
    correo.trim().length > 0 &&
    area.trim().length > 0 &&
    password.trim().length > 0 &&
    !busy;

  const onSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setBusy(true);
    setError(null);
    setSuccess(false);

    try {
      const payload: RequestUserPayload = {
        nombre: nombre.trim(),
        correo: correo.trim(),
        area: area.trim(),
        password: password.trim(),
      };

      const res = await fetch("/api/auth/request-user", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        credentials: "include",
        body: JSON.stringify(payload),
      });

      if (!res.ok) {
        throw new Error("No se pudo enviar la solicitud.");
      }

      setSuccess(true);
      setNombre("");
      setCorreo("");
      setArea("");
      setPassword("");
    } catch {
      setError("No se pudo enviar la solicitud.");
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className={s.root}>
      <Card className={s.card}>
        <CardHeader
          header={<Text weight="semibold" size={600}>Crear usuario</Text>}
          description={<Text size={300}>Solicitud de alta para acceso al sistema</Text>}
        />
        <CardPreview>
          <div className={s.inner}>
            <form onSubmit={onSubmit} className={s.form}>
              <div className={s.row}>
                <Text>Nombre</Text>
                <Input
                  appearance="outline"
                  size="large"
                  className={s.input}
                  value={nombre}
                  onChange={(_, d) => setNombre(d.value)}
                  placeholder="Ingresa tu nombre"
                />
              </div>

              <div className={s.row}>
                <Text>Correo</Text>
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
                <Text>Área</Text>
                <Input
                  appearance="outline"
                  size="large"
                  className={s.input}
                  value={area}
                  onChange={(_, d) => setArea(d.value)}
                  placeholder="Ej. Almacen"
                />
              </div>

              <div className={s.row}>
                <Text>Contraseña</Text>
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
                <div className={s.success}>
                  Solicitud enviada, el administrador la revisará
                </div>
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
        </CardPreview>
      </Card>
    </div>
  );
}
