import type { CSSProperties } from "react";

/** Estilo base para tablas legibles con columnas proporcionales. */
export const TABLE_FIXED_STYLE: CSSProperties = {
  width: "100%",
  tableLayout: "fixed",
};

/** Truncado en celdas dentro de table-layout: fixed (maxWidth: 0 activa ellipsis). */
export const TRUNCATE_CELL: CSSProperties = {
  overflow: "hidden",
  textOverflow: "ellipsis",
  whiteSpace: "nowrap",
  maxWidth: 0,
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
