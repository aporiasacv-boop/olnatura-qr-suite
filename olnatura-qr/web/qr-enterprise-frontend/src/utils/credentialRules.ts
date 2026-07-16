/** Reglas de correo corporativo y contraseña (alta de usuario). */

export const ALLOWED_EMAIL_DOMAIN = "@olnatura.com";

const BASIC_EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export type PasswordCheck = {
  minLength: boolean;
  upper: boolean;
  lower: boolean;
  digit: boolean;
};

export function isAllowedEmail(email: string): boolean {
  const e = (email ?? "").trim().toLowerCase();
  if (!BASIC_EMAIL.test(e)) return false;
  return e.endsWith(ALLOWED_EMAIL_DOMAIN);
}

export function emailValidationMessage(email: string): string | null {
  const raw = (email ?? "").trim();
  if (!raw) return "El correo es obligatorio.";
  const e = raw.toLowerCase();
  if (!BASIC_EMAIL.test(e)) return "El correo no tiene un formato válido.";
  if (!e.endsWith(ALLOWED_EMAIL_DOMAIN)) {
    return "Solo se permiten correos @olnatura.com.";
  }
  return null;
}

export function passwordChecks(password: string): PasswordCheck {
  const p = password ?? "";
  return {
    minLength: p.length >= 8,
    upper: /[A-Z]/.test(p),
    lower: /[a-z]/.test(p),
    digit: /[0-9]/.test(p),
  };
}

export function isValidPassword(password: string): boolean {
  const c = passwordChecks(password);
  return c.minLength && c.upper && c.lower && c.digit;
}

export function passwordValidationMessage(password: string): string | null {
  if (!(password ?? "").length) return "La contraseña es obligatoria.";
  const c = passwordChecks(password);
  const missing: string[] = [];
  if (!c.minLength) missing.push("mínimo 8 caracteres");
  if (!c.upper) missing.push("una mayúscula");
  if (!c.lower) missing.push("una minúscula");
  if (!c.digit) missing.push("un número");
  if (missing.length === 0) return null;
  return `La contraseña debe incluir: ${missing.join(", ")}.`;
}

export const PASSWORD_RULE_LABELS: { key: keyof PasswordCheck; label: string }[] = [
  { key: "minLength", label: "Mínimo 8 caracteres" },
  { key: "upper", label: "Una letra mayúscula" },
  { key: "lower", label: "Una letra minúscula" },
  { key: "digit", label: "Un número" },
];
