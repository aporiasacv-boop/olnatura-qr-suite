// src/pages/RequestAccessPage.tsx
import * as React from "react";
import {
  Button,
  Card,
  CardHeader,
  CardPreview,
  Dropdown,
  Input,
  Option,
  Text,
  makeStyles,
  shorthands,
} from "@fluentui/react-components";

import { api, ApiError } from "../api/client";
import type { RequestAccessPayload, RequestAccessResponse } from "../api/types";
import { useToasts } from "../components/ui/toasts";
import { brand } from "../styles/brand";
import { useNavigate } from "react-router-dom";

const useStyles = makeStyles({
  root: {
    minHeight: "100vh",
    display: "grid",
    placeItems: "center",
    backgroundColor: brand.background,
    ...shorthands.padding("24px"),
  },
  card: {
    width: "520px",
    maxWidth: "100%",
    ...shorthands.border("1px", "solid", brand.border),
    ...shorthands.borderRadius("14px"),
  },
  inner: {
    display: "grid",
    rowGap: "12px",
    ...shorthands.padding("18px"),
  },
  row: { display: "grid", rowGap: "6px" },
  muted: { color: brand.muted },
  actions: { display: "flex", gap: "10px", justifyContent: "flex-end" },
});

const ROLE_OPTIONS = [
  { value: "ALMACEN", label: "ALMACÉN" },
  { value: "INSPECCION", label: "INSPECCIÓN" },
] as const;

export default function RequestAccessPage() {
  const s = useStyles();
  const toasts = useToasts();
  const nav = useNavigate();

  const [username, setUsername] = React.useState("");
  const [email, setEmail] = React.useState("");
  const [password, setPassword] = React.useState("");
  const [roleRequested, setRoleRequested] = React.useState<
    RequestAccessPayload["roleRequested"] | ""
  >("");

  const [busy, setBusy] = React.useState(false);
  const [submitted, setSubmitted] = React.useState<RequestAccessResponse | null>(null);

  const canSubmit =
    username.trim().length > 0 &&
    email.trim().length > 0 &&
    password.length > 0 &&
    roleRequested !== "" &&
    !busy;

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setBusy(true);

    try {
      const payload: RequestAccessPayload = {
        username: username.trim(),
        email: email.trim(),
        password,
        roleRequested: roleRequested as RequestAccessPayload["roleRequested"],
      };

      const res = await api<RequestAccessResponse>("/auth/request-access", {
        method: "POST",
        body: JSON.stringify(payload),
      });

      setSubmitted(res);

      toasts.push({
        intent: "success",
        title: "Solicitud enviada",
        message: "Quedó en estado PENDING. Un admin la revisará.",
      });
    } catch (err: any) {
      const ae = err as ApiError;
      toasts.push({
        intent: "error",
        title: "No se pudo enviar la solicitud",
        message: "Revisa los datos o inténtalo nuevamente.",
        error: ae,
      });
    } finally {
      setBusy(false);
    }
  };

  if (submitted) {
    return (
      <div className={s.root}>
        <Card className={s.card}>
          <CardHeader
            header={<Text weight="semibold" size={600}>Solicitud enviada</Text>}
            description={<Text size={300} className={s.muted}>Estado: {submitted.status}</Text>}
          />
          <CardPreview>
            <div className={s.inner}>
              <Text>
                Tu solicitud fue registrada. Cuando un admin la apruebe, podrás iniciar sesión.
              </Text>
              <div className={s.actions}>
                <Button appearance="primary" onClick={() => nav("/login", { replace: true })}>
                  Volver a Login
                </Button>
              </div>
            </div>
          </CardPreview>
        </Card>
      </div>
    );
  }

  return (
    <div className={s.root}>
      <Card className={s.card}>
        <CardHeader
          header={<Text weight="semibold" size={600}>Request Access</Text>}
          description={<Text size={300} className={s.muted}>Solicita acceso al sistema interno</Text>}
        />
        <CardPreview>
          <div className={s.inner}>
            <form onSubmit={onSubmit} style={{ display: "grid", rowGap: "12px" }}>
              <div className={s.row}>
                <Text>Username</Text>
                <Input value={username} onChange={(_, d) => setUsername(d.value)} />
              </div>

              <div className={s.row}>
                <Text>Email</Text>
                <Input value={email} onChange={(_, d) => setEmail(d.value)} />
              </div>

              <div className={s.row}>
                <Text>Password</Text>
                <Input type="password" value={password} onChange={(_, d) => setPassword(d.value)} />
              </div>

              <div className={s.row}>
                <Text>Role requested</Text>
                <Dropdown
                  placeholder="Selecciona un rol"
                  selectedOptions={roleRequested ? [roleRequested] : []}
                  onOptionSelect={(_, data) =>
                    setRoleRequested((data.optionValue ?? "") as any)
                  }
                >
                  {ROLE_OPTIONS.map((r) => (
                    <Option key={r.value} value={r.value}>
                      {r.label}
                    </Option>
                  ))}
                </Dropdown>
              </div>

              <div className={s.actions}>
                <Button appearance="subtle" onClick={() => nav("/login")}>
                  Cancelar
                </Button>
                <Button appearance="primary" type="submit" disabled={!canSubmit}>
                  {busy ? "Enviando…" : "Enviar solicitud"}
                </Button>
              </div>
            </form>
          </div>
        </CardPreview>
      </Card>
    </div>
  );
}