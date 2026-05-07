import { useMemo, useState } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import {
  Button,
  Input,
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
    width: "420px",
    maxWidth: "100%",
    backgroundColor: brand.surface,
    borderRadius: "12px",
    boxShadow: "0 2px 8px rgba(0,0,0,0.06)",
    ...shorthands.border("1px", "solid", brand.border),
    ...shorthands.padding("24px"),
  },
  header: {
    marginBottom: "24px",
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
  brandRow: {
    display: "flex",
    gap: "10px",
    alignItems: "center",
    marginBottom: "20px",
  },
  logo: {
    width: "32px",
    height: "32px",
    objectFit: "contain",
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
  input: {
    minHeight: "40px",
    width: "100%",
  },
  err: {
    color: brand.dangerFg,
    fontSize: "13px",
  },
  primaryButton: {
    width: "100%",
    minHeight: "40px",
    borderRadius: "8px",
  },
  secondaryButton: {
    width: "100%",
    minHeight: "40px",
    borderRadius: "8px",
  },
  meta: {
    color: brand.muted,
    fontSize: "12px",
    textAlign: "center",
    marginTop: "16px",
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
      <div className={s.card}>
        <div className={s.header}>
          <div className={s.brandRow}>
            <img src="/logo-olnatura.png" alt="Logo" className={s.logo} />
            <div>
              <div className={s.title}>Sistema Olnatura</div>
              <div className={s.subtitle}>Plataforma de trazabilidad QR</div>
            </div>
          </div>
        </div>

        <form onSubmit={onSubmit} className={s.form}>
          <div className={s.row}>
            <span className={s.label}>Usuario</span>
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
            <span className={s.label}>Contraseña</span>
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

          <div className={s.meta}>© 2026 OLNATURA. Todos los derechos reservados.</div>
        </form>
      </div>
    </div>
  );
}
