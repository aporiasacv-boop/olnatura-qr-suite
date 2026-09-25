const FLEX_DMY_REGEX = /^(\d{1,2})\/(\d{1,2})\/(\d{2}|\d{4})$/;

function parseFlexibleDMY(trimmed: string): { d: number; m: number; y: number } | null {
  const m = trimmed.match(FLEX_DMY_REGEX);
  if (!m) return null;
  const day = parseInt(m[1]!, 10);
  const month = parseInt(m[2]!, 10);
  let year = parseInt(m[3]!, 10);
  if (m[3]!.length === 2) year = 2000 + year;
  if (month < 1 || month > 12 || day < 1 || day > 31) return null;
  const date = new Date(year, month - 1, day);
  if (date.getFullYear() !== year || date.getMonth() !== month - 1 || date.getDate() !== day) return null;
  return { d: day, m: month, y: year };
}


export function formatDateDDMMYYYY(isoOrLocal: string | null | undefined): string {
  if (!isoOrLocal || typeof isoOrLocal !== "string") return "";
  const trimmed = isoOrLocal.trim();
  if (!trimmed) return "";
  const d = parseToDate(trimmed);
  if (!d || isNaN(d.getTime())) return trimmed;
  const day = String(d.getDate()).padStart(2, "0");
  const month = String(d.getMonth() + 1).padStart(2, "0");
  const year = d.getFullYear();
  return `${day}/${month}/${year}`;
}

const MESES_ETIQUETA = ["ENE", "FEB", "MAR", "ABR", "MAY", "JUN", "JUL", "AGO", "SEP", "OCT", "NOV", "DIC"];

export function formatDateLabelDDMMMYY(isoOrLocal: string | null | undefined): string {
  if (!isoOrLocal || typeof isoOrLocal !== "string") return "";
  const trimmed = isoOrLocal.trim();
  if (!trimmed) return "";
  const iso = trimmed.match(/^(\d{4})-(\d{2})-(\d{2})/);
  if (iso) {
    const day = iso[3];
    const mon = MESES_ETIQUETA[Number(iso[2]) - 1] ?? "";
    const year = iso[1].slice(-2);
    return `${day}/${mon}/${year}`;
  }
  const r = parseFlexibleDMY(trimmed);
  if (!r) return "";
  const day = String(r.d).padStart(2, "0");
  const mon = MESES_ETIQUETA[r.m - 1] ?? "";
  const year = String(r.y).slice(-2);
  return `${day}/${mon}/${year}`;
}

export function parseDDMMYYYYToISO(input: string | null | undefined): string {
  if (!input || typeof input !== "string") return "";
  const r = parseFlexibleDMY(input.trim());
  if (!r) return "";
  const y = r.y;
  const mo = String(r.m).padStart(2, "0");
  const d = String(r.d).padStart(2, "0");
  return `${y}-${mo}-${d}`;
}

function parseToDate(s: string): Date | null {
  const iso = s.match(/^(\d{4})-(\d{2})-(\d{2})(?:$|T)/);
  if (iso) {
    const year = Number(iso[1]);
    const month = Number(iso[2]);
    const day = Number(iso[3]);
    const date = new Date(year, month - 1, day);
    if (date.getFullYear() !== year || date.getMonth() !== month - 1 || date.getDate() !== day) {
      return null;
    }
    return date;
  }
  const r = parseFlexibleDMY(s.trim());
  if (r) return new Date(r.y, r.m - 1, r.d);
  return new Date(s);
}

export function isValidDDMMYYYY(input: string | null | undefined): boolean {
  if (!input || typeof input !== "string") return false;
  const iso = parseDDMMYYYYToISO(input.trim());
  return iso.length === 10;
}


export function isoToDisplay(iso: string | null | undefined): string {
  return formatDateDDMMYYYY(iso);
}

export function fechaTipoEtiqueta(
  caducidad: unknown,
  reanalisis: unknown
): "REANALISIS" | "CADUCIDAD" | null {
  if (storedDateText(reanalisis)) return "REANALISIS";
  if (storedDateText(caducidad)) return "CADUCIDAD";
  return null;
}

export function storedDateText(value: unknown): string {
  if (value == null) return "";
  if (typeof value === "string") return value.trim();
  if (Array.isArray(value) && value.length >= 3) {
    const year = Number(value[0]);
    const month = Number(value[1]);
    const day = Number(value[2]);
    if (!Number.isFinite(year) || !Number.isFinite(month) || !Number.isFinite(day)) return "";
    return `${String(year).padStart(4, "0")}-${String(month).padStart(2, "0")}-${String(day).padStart(2, "0")}`;
  }
  return String(value).trim();
}
