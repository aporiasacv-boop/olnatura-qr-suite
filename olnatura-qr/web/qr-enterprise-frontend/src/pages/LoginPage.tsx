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

const useStyles = makeStyles({
  root: {
    minHeight: "100vh",
    display: "grid",
    gridTemplateRows: "1fr auto 1fr",
    backgroundColor: "#F6F7F8",
    ...shorthands.padding("24px"),
  },
  center: {
    gridRow: 2,
    display: "flex",
    justifyContent: "center",
  },
  card: {
    width: "460px",
    maxWidth: "100%",
  },
  inner: {
    display: "grid",
    rowGap: "12px",
    ...shorthands.padding("20px"),
  },
  row: {
    display: "grid",
    rowGap: "6px",
  },
  meta: {
    color: "#6B6B6B",
    fontSize: "12px",
    ...shorthands.margin("8px", "0", "0"),
  },
  err: {
    color: "#B10E1C",
  },
  brandRow: {
    display: "flex",
    gap: "10px",
    alignItems: "center",
  },
  logo: {
    width: "28px",
    height: "28px",
    objectFit: "contain",
    display: "block",
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
      <div className={s.center}>
        <Card className={s.card}>
          <CardHeader
            header={
              <Text weight="semibold" size={600}>
                Inventory System
              </Text>
            }
            description={<Text size={300}>QR Traceability Platform</Text>}
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

              <form onSubmit={onSubmit} style={{ display: "grid", rowGap: "12px" }}>
                <div className={s.row}>
                  <Text>Username</Text>
                  <Input
                    value={username}
                    onChange={(_, d) => setUsername(d.value)}
                    placeholder="Enter username"
                  />
                </div>

                <div className={s.row}>
                  <Text>Password</Text>
                  <Input
                    type="password"
                    value={password}
                    onChange={(_, d) => setPassword(d.value)}
                    placeholder="Enter password"
                  />
                </div>

                {error ? <div className={s.err}>{error}</div> : null}

                <Button
                  appearance="primary"
                  type="submit"
                  disabled={busy || !username || !password}
                >
                  {busy ? "Signing in…" : "Sign in"}
                </Button>

                <div className={s.meta}>
                  <Text>© 2026 OLNATURA. All rights reserved.</Text>
                </div>
              </form>
            </div>
          </CardPreview>
        </Card>
      </div>
    </div>
  );
}