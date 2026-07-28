import type { CSSProperties } from "react";

/** Estilo base para tablas legibles con columnas proporcionales. */
export const TABLE_FIXED_STYLE: CSSProperties = {
  width: "100%",
  tableLayout: "fixed",
};

/**
 * Clase CSS (global.css) que reduce solo la fuente del tbody,
 * sin afectar encabezados ni el resto de la UI.
 */
export const TABLE_DATA_CLASS = "app-data-table";

/** Truncado en celdas secundarias (maxWidth: 0 activa ellipsis en table-layout: fixed). */
export const TRUNCATE_CELL: CSSProperties = {
  overflow: "hidden",
  textOverflow: "ellipsis",
  whiteSpace: "nowrap",
  maxWidth: 0,
};

/**
 * Truncado prioritario para Lote / Usuario:
 * una sola línea, pero sin forzar maxWidth:0 para que el % de columna muestre más caracteres.
 */
export const TRUNCATE_CELL_PRIORITY: CSSProperties = {
  overflow: "hidden",
  textOverflow: "ellipsis",
  whiteSpace: "nowrap",
};

export function cellTitle(value: string | null | undefined): string | undefined {
  if (value == null) return undefined;
  const trimmed = String(value).trim();
  if (!trimmed || trimmed === "—") return undefined;
  return trimmed;
}

/** Contenedor con scroll horizontal solo cuando la tabla lo requiere. */
export const TABLE_SCROLL_WRAP: CSSProperties = {
  overflowX: "auto",
  width: "100%",
};
