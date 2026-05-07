

/** d/M/yy, dd/MM/yy, d/MM/yyyy, dd/M/yyyy, dd/MM/yyyy */
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

<<<<<<< HEAD

=======
/**
 * Parse flexible d/M/y forms to ISO string (YYYY-MM-DD) for API.
 * Accepts: d/M/yy, dd/MM/yy, d/MM/yyyy, dd/M/yyyy, dd/MM/yyyy (yy → 20yy).
 */
>>>>>>> origin/cleanup/repo-sanitize
export function parseDDMMYYYYToISO(input: string | null | undefined): string {
  if (!input || typeof input !== "string") return "";
  const r = parseFlexibleDMY(input.trim());
  if (!r) return "";
  const y = r.y;
  const mo = String(r.m).padStart(2, "0");
  const d = String(r.d).padStart(2, "0");
  return `${y}-${mo}-${d}`;
}

<<<<<<< HEAD

=======
/**
 * Parse YYYY-MM-DD or slash-separated d/M/y to Date
 */
>>>>>>> origin/cleanup/repo-sanitize
function parseToDate(s: string): Date | null {
  const iso = /^\d{4}-\d{2}-\d{2}/.test(s);
  if (iso) return new Date(s);
  const r = parseFlexibleDMY(s.trim());
  if (r) return new Date(r.y, r.m - 1, r.d);
  return new Date(s);
}

<<<<<<< HEAD

=======
/**
 * Validate date input (flexible forms accepted)
 */
>>>>>>> origin/cleanup/repo-sanitize
export function isValidDDMMYYYY(input: string | null | undefined): boolean {
  if (!input || typeof input !== "string") return false;
  const iso = parseDDMMYYYYToISO(input.trim());
  return iso.length === 10;
}


export function isoToDisplay(iso: string | null | undefined): string {
  return formatDateDDMMYYYY(iso);
}
