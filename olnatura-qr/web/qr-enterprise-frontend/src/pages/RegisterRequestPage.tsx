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
import BrandLogo from "../components/ui/BrandLogo";
import PasswordField from "../components/ui/PasswordField";
import {
  emailValidationMessage,
  isAllowedEmail,
  isValidPassword,
  passwordChecks,
  PASSWORD_RULE_LABELS,
} from "../utils/credentialRules";

const useStyles = makeStyles({
  root: {
    minHeight: "100vh",
    display: "grid",
    placeItems: "center",
    backgroundColor: "transparent",
    ...shorthands.padding("24px"),
  },
  card: {
    width: "440px",
    maxWidth: "100%",
    backgroundColor: brand.surface,
    backdropFilter: "blur(10px)",
    borderRadius: "12px",
    boxShadow: "0 8px 28px rgba(74, 92, 40, 0.10)",
    ...shorthands.border("1px", "solid", brand.border),
    ...shorthands.padding("24px"),
  },
  header: {
    marginBottom: "20px",
    display: "grid",
    gap: "14px",
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
  hintOk: {
    fontSize: "12px",
    color: brand.successFg,
  },
  hintErr: {
    fontSize: "12px",
    color: brand.dangerFg,
  },
  hintMuted: {
    fontSize: "12px",
    color: brand.muted,
  },
  rules: {
    display: "grid",
    gap: "4px",
    marginTop: "2px",
  },
  ruleOk: {
    fontSize: "12px",
    color: brand.successFg,
  },
  rulePending: {
    fontSize: "12px",
    color: brand.muted,
  },
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
  roleRequested: "ALMACEN" | "PRODUCCION" | "CALIDAD" | "INSPECCION";
  password: string;
};

export default function RegisterRequestPage() {
  const s = useStyles();
  const nav = useNavigate();

  const [nombre, setNombre] = React.useState("");
  const [correo, setCorreo] = React.useState("");
  const [area, setArea] = React.useState<"ALMACEN" | "PRODUCCION" | "CALIDAD" | "INSPECCION" | "">("");
  const [password, setPassword] = React.useState("");
  const [busy, setBusy] = React.useState(false);
  const [error, setError] = React.useState<string | null>(null);
  const [success, setSuccess] = React.useState(false);
  const [touchedEmail, setTouchedEmail] = React.useState(false);
  const [touchedPassword, setTouchedPassword] = React.useState(false);

  const emailMsg = emailValidationMessage(correo);
  const emailOk = isAllowedEmail(correo);
  const pwdChecks = passwordChecks(password);
  const passwordOk = isValidPassword(password);
  const showEmailHint = touchedEmail || correo.trim().length > 0;
  const showPasswordHints = touchedPassword || password.length > 0;

  const canSubmit =
    nombre.trim().length > 0 &&
    emailOk &&
    passwordOk &&
    area !== "" &&
    !busy;

  const onSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setTouchedEmail(true);
    setTouchedPassword(true);
    setBusy(true);
    setError(null);
    setSuccess(false);

    try {
      if (area === "") {
        setError("Selecciona un área.");
        setBusy(false);
        return;
      }
      if (!emailOk) {
        setError(emailMsg || "Correo inválido.");
        setBusy(false);
        return;
      }
      if (!passwordOk) {
        setError("La contraseña no cumple los requisitos.");
        setBusy(false);
        return;
      }

      const payload: RequestUserPayload = {
        username: nombre.trim(),
        email: correo.trim().toLowerCase(),
        roleRequested: area,
        password: password,
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
      setTouchedEmail(false);
      setTouchedPassword(false);
    } catch (err) {
      const ae = err as ApiError;
      setError(ae?.message || "No se pudo enviar la solicitud.");
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className={s.root}>
      <div className={`${s.card} app-card`}>
        <div className={s.header}>
          <BrandLogo size={48} title="Sistema Olnatura" subtitle="QR Suite" />
          <div>
            <div className={s.title}>Crear usuario</div>
            <div className={s.subtitle}>Solicitud de alta para acceso al sistema</div>
          </div>
        </div>

        <form onSubmit={onSubmit} className={s.form} noValidate>
          <div className={s.row}>
            <span className={s.label}>Usuario</span>
            <Input
              appearance="outline"
              size="large"
              className={s.input}
              value={nombre}
              onChange={(_, d) => setNombre(d.value)}
              placeholder="Ingresa tu usuario"
              autoComplete="username"
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
              onBlur={() => setTouchedEmail(true)}
              placeholder="nombre@olnatura.com"
              type="email"
              autoComplete="email"
            />
            {showEmailHint ? (
              emailOk ? (
                <span className={s.hintOk}>Correo corporativo válido.</span>
              ) : (
                <span className={s.hintErr}>{emailMsg}</span>
              )
            ) : (
              <span className={s.hintMuted}>Solo correos @olnatura.com.</span>
            )}
          </div>

          <div className={s.row}>
            <span className={s.label}>Área</span>
            <Dropdown
              appearance="outline"
              size="large"
              placeholder="Selecciona un área"
              selectedOptions={area ? [area] : []}
              onOptionSelect={(_, data) =>
                setArea((data.optionValue ?? "") as "ALMACEN" | "PRODUCCION" | "CALIDAD" | "INSPECCION" | "")
              }
            >
              <Option value="ALMACEN">ALMACÉN</Option>
              <Option value="PRODUCCION">PRODUCCIÓN</Option>
              <Option value="CALIDAD">CONTROL DE CALIDAD</Option>
              <Option value="INSPECCION">INSPECCIÓN</Option>
            </Dropdown>
          </div>

          <div className={s.row}>
            <span className={s.label}>Contraseña</span>
            <PasswordField
              value={password}
              onChange={(v) => {
                setPassword(v);
                setTouchedPassword(true);
              }}
              placeholder="Ingresa una contraseña"
              className={s.input}
              autoComplete="new-password"
            />
            {showPasswordHints ? (
              <div className={s.rules} aria-live="polite">
                {PASSWORD_RULE_LABELS.map(({ key, label }) => (
                  <span key={key} className={pwdChecks[key] ? s.ruleOk : s.rulePending}>
                    {pwdChecks[key] ? "✓" : "○"} {label}
                  </span>
                ))}
              </div>
            ) : (
              <span className={s.hintMuted}>
                Mínimo 8 caracteres, con mayúscula, minúscula y número.
              </span>
            )}
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
