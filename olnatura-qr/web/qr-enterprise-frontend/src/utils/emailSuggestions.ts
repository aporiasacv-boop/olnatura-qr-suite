/**
 * Sugerencias de correo corporativo para "Crear usuario".
 * Para agregar más correos, incluirlos en SUGGESTED_EMAILS.
 * Los ya utilizados (alta exitosa o conflicto 409) se ocultan vía localStorage.
 */
export const SUGGESTED_EMAILS: readonly string[] = [
  "Virginia.Amaro@olnatura.com",
  "ac.supervision@olnatura.com",
  "supervisor.inspeccion@olnatura.com",
  "inspeccion.materiales@olnatura.com",
] as const;

const STORAGE_KEY = "olnatura_used_email_suggestions_v1";

function normalize(email: string): string {
  return email.trim().toLowerCase();
}

export function loadUsedEmailSuggestions(): Set<string> {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return new Set();
    const parsed = JSON.parse(raw) as unknown;
    if (!Array.isArray(parsed)) return new Set();
    return new Set(parsed.map((e) => normalize(String(e))).filter(Boolean));
  } catch {
    return new Set();
  }
}

export function markEmailSuggestionUsed(email: string): void {
  const n = normalize(email);
  if (!n) return;
  const used = loadUsedEmailSuggestions();
  used.add(n);
  localStorage.setItem(STORAGE_KEY, JSON.stringify([...used]));
}

export function filterEmailSuggestions(query: string, used: Set<string> = loadUsedEmailSuggestions()): string[] {
  const q = normalize(query);
  return SUGGESTED_EMAILS.filter((email) => {
    const lower = normalize(email);
    if (used.has(lower)) return false;
    return q.length === 0 || lower.includes(q);
  });
}
