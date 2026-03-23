import * as React from "react";
import {
  Button,
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
    width: "440px",
    maxWidth: "100%",
    backgroundColor: brand.surface,
    borderRadius: "12px",
    boxShadow: "0 2px 8px rgba(0,0,0,0.06)",
    ...shorthands.border("1px", "solid", brand.border),
    ...shorthands.padding("24px"),
  },
  header: { marginBottom: "20px" },
  title: { fontSize: "20px", fontWeight: 600, color: brand.text },
  subtitle: { fontSize: "14px", color: brand.muted, marginTop: "4px" },
  form: { display: "grid", rowGap: "16px" },
  row: { display: "grid", rowGap: "8px" },
  label: { fontSize: "14px", fontWeight: 500, color: brand.text2 },
  input: { width: "100%", minWidth: 0 },
  actions: { display: "flex", gap: "10px", justifyContent: "flex-end", marginTop: "8px" },
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
  const [roleRequested, setRoleRequested] = React.useState<RequestAccessPayload["roleRequested"] | "">("");
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
        body: payload,
      });
      setSubmitted(res);
      toasts.push({
        intent: "success",
        title: "Solicitud enviada",
        message: "Tu solicitud fue enviada correctamente.",
      });
    } catch (err: unknown) {
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
        <div className={s.card}>
          <div className={s.header}>
            <div className={s.title}>Solicitud enviada</div>
            <div className={s.subtitle}>En revisión</div>
          </div>
          <Text style={{ display: "block", marginBottom: 16 }}>
            Tu solicitud fue registrada. Cuando un administrador la apruebe, podrás iniciar sesión.
          </Text>
          <Button appearance="primary" onClick={() => nav("/login", { replace: true })}>
            Volver al inicio de sesión
          </Button>
        </div>
      </div>
    );
  }

  return (
    <div className={s.root}>
      <div className={s.card}>
        <div className={s.header}>
          <div className={s.title}>Solicitar acceso</div>
          <div className={s.subtitle}>Completa los datos para enviar tu solicitud</div>
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
              placeholder="Usuario"
            />
          </div>
          <div className={s.row}>
            <span className={s.label}>Correo electrónico</span>
            <Input
              appearance="outline"
              size="large"
              className={s.input}
              value={email}
              onChange={(_, d) => setEmail(d.value)}
              placeholder="correo@ejemplo.com"
            />
          </div>
          <div className={s.row}>
            <span className={s.label}>Contraseña</span>
            <Input
              appearance="outline"
              size="large"
              type="password"
              className={s.input}
              value={password}
              onChange={(_, d) => setPassword(d.value)}
            />
          </div>
          <div className={s.row}>
            <span className={s.label}>Rol solicitado</span>
            <Dropdown
              appearance="outline"
              size="large"
              placeholder="Selecciona un rol"
              selectedOptions={roleRequested ? [roleRequested] : []}
              onOptionSelect={(_, data) =>
                setRoleRequested((data.optionValue ?? "") as RequestAccessPayload["roleRequested"])
              }
            >
              {ROLE_OPTIONS.map((r) => (
                <Option key={r.value} value={r.value}>{r.label}</Option>
              ))}
            </Dropdown>
          </div>
          <div className={s.actions}>
            <Button appearance="secondary" onClick={() => nav("/login")}>Cancelar</Button>
            <Button appearance="primary" type="submit" disabled={!canSubmit}>
              {busy ? "Enviando…" : "Enviar solicitud"}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}
