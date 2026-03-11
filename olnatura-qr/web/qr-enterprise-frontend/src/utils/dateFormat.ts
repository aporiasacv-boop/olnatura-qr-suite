/**
 * Date formatting for Microsoft Dynamics compatibility: DD/MM/YYYY
 * Example: 11/09/2025
 * Used in label preview, PNG export, ZPL, and form display.
 * All dates shown to users must use this format.
 */

const DDMMYYYY_REGEX = /^(\d{1,2})\/(\d{1,2})\/(\d{4})$/;

/**
 * Format any date string (YYYY-MM-DD or DD/MM/YYYY) to DD/MM/YYYY for display
 */
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

/**
 * Parse DD/MM/YYYY to ISO string (YYYY-MM-DD) for API
 */
export function parseDDMMYYYYToISO(input: string | null | undefined): string {
  if (!input || typeof input !== "string") return "";
  const m = input.trim().match(DDMMYYYY_REGEX);
  if (!m) return "";
  const [, d, mo, y] = m;
  const day = parseInt(d!, 10);
  const month = parseInt(mo!, 10);
  const year = parseInt(y!, 10);
  if (month < 1 || month > 12 || day < 1 || day > 31) return "";
  const date = new Date(year, month - 1, day);
  if (date.getFullYear() !== year || date.getMonth() !== month - 1 || date.getDate() !== day)
    return "";
  return date.toISOString().slice(0, 10);
}

/**
 * Parse YYYY-MM-DD or DD/MM/YYYY to Date
 */
function parseToDate(s: string): Date | null {
  const iso = /^\d{4}-\d{2}-\d{2}/.test(s);
  if (iso) return new Date(s);
  const m = s.match(DDMMYYYY_REGEX);
  if (m) {
    const [, d, mo, y] = m;
    return new Date(parseInt(y!, 10), parseInt(mo!, 10) - 1, parseInt(d!, 10));
  }
  return new Date(s);
}

/**
 * Validate DD/MM/YYYY format
 */
export function isValidDDMMYYYY(input: string | null | undefined): boolean {
  if (!input || typeof input !== "string") return false;
  const iso = parseDDMMYYYYToISO(input.trim());
  return iso.length === 10;
}

/**
 * Convert ISO (YYYY-MM-DD) from API to display string DD/MM/YYYY
 */
export function isoToDisplay(iso: string | null | undefined): string {
  return formatDateDDMMYYYY(iso);
}
