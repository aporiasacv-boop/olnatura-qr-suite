import { useMemo, useState } from "react";
import { useNavigate, useLocation } from "react-router-dom";
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
import { useAuth } from "../auth/AuthContext";
import { ApiError } from "../api/client";
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
    width: "440px",
    maxWidth: "100%",
    ...shorthands.border("1px", "solid", brand.border),
    ...shorthands.borderRadius("14px"),
  },
  inner: {
    display: "grid",
    rowGap: "14px",
    ...shorthands.padding("20px", "20px", "18px"),
  },
  row: {
    display: "grid",
    rowGap: "8px",
  },
  meta: {
    color: brand.muted,
    fontSize: "12px",
    textAlign: "center",
    ...shorthands.margin("6px", "0", "0"),
  },
  err: {
    color: brand.dangerFg,
    fontSize: "13px",
  },
  brandRow: {
    display: "flex",
    gap: "8px",
    alignItems: "center",
    ...shorthands.margin("0", "0", "4px"),
  },
  logo: {
    width: "28px",
    height: "28px",
    objectFit: "contain",
    display: "block",
  },
  form: {
    display: "grid",
    rowGap: "14px",
  },
  input: {
    width: "100%",
    minWidth: 0,
  },
  primaryButton: {
    width: "100%",
    minWidth: 0,
  },
  secondaryButton: {
    width: "100%",
    minWidth: 0,
  },
});

export default function LoginPage() {
  const s = useStyles();
  const { login } = useAuth();
  const nav = useNavigate();
  const loc = useLocation() as any;

  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const redirectTo = useMemo(() => loc.state?.from ?? "/", [loc.state]);

  const onSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await login({ username, password });
      nav(redirectTo, { replace: true });
    } catch (err) {
      const ae = err as ApiError;
      setError(
        ae.status === 401
          ? "Credenciales inválidas o cuenta no autorizada."
          : "No se pudo iniciar sesión."
      );
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className={s.root}>
      <Card className={s.card}>
        <CardHeader
          header={
            <Text weight="semibold" size={600}>
              Sistema Olnatura
            </Text>
          }
          description={<Text size={300}>Plataforma de trazabilidad QR</Text>}
        />
        <CardPreview>
          <div className={s.inner}>
            <div className={s.brandRow}>
              <img
                src="/logo-olnatura.png"
                alt="Logo"
                className={s.logo}
              />
              <Text weight="semibold">Acceso</Text>
            </div>

            <form onSubmit={onSubmit} className={s.form}>
              <div className={s.row}>
                <Text>Usuario</Text>
                <Input
                  appearance="outline"
                  size="large"
                  className={s.input}
                  value={username}
                  onChange={(_, d) => setUsername(d.value)}
                  placeholder="Ingresa tu usuario"
                />
              </div>

              <div className={s.row}>
                <Text>Contraseña</Text>
                <Input
                  appearance="outline"
                  size="large"
                  className={s.input}
                  type="password"
                  value={password}
                  onChange={(_, d) => setPassword(d.value)}
                  placeholder="Ingresa tu contraseña"
                />
              </div>

              {error ? <div className={s.err}>{error}</div> : null}

              <Button
                className={s.primaryButton}
                appearance="primary"
                size="large"
                type="submit"
                disabled={busy || !username || !password}
              >
                {busy ? "Iniciando sesión..." : "Iniciar sesión"}
              </Button>

              <Button
                className={s.secondaryButton}
                appearance="secondary"
                size="large"
                type="button"
                onClick={() => nav("/register-request")}
              >
                Crear usuario
              </Button>

              <div className={s.meta}>
                <Text>© 2026 OLNATURA. Todos los derechos reservados.</Text>
              </div>
            </form>
          </div>
        </CardPreview>
      </Card>
    </div>
  );
}