
export const APP_NUMBER_LOCALE = "es-MX";


export function formatNumber(
  value: number | string | null | undefined,
  options?: Intl.NumberFormatOptions
): string {
  if (value == null || value === "") return "—";
  const n = typeof value === "number" ? value : Number(String(value).trim().replace(/,/g, ""));
  if (Number.isNaN(n)) {
    const raw = String(value).trim();
    return raw.length > 0 ? raw : "—";
  }
  return n.toLocaleString(APP_NUMBER_LOCALE, options);
}
