/** Formato numérico de la aplicación (separadores de miles). */
export const APP_NUMBER_LOCALE = "es-MX";

/**
 * Formatea un número para visualización con separadores de miles.
 * Si el valor no es numérico, lo devuelve como texto (salvo vacío → "—").
 */
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

/** Cantidad + unidad opcional (p. ej. inventario disponible). */
export function formatQuantity(
  value: number | string | null | undefined,
  unit?: string | null
): string {
  const qty = formatNumber(value);
  if (qty === "—") return "—";
  const u = unit != null ? String(unit).trim() : "";
  return u ? `${qty} ${u}` : qty;
}
