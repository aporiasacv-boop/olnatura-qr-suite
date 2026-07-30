import type { CSSProperties } from "react";

/** Layout fijo + scroll horizontal cuando el contenido no cabe. */
export const TABLE_FIXED_STYLE: CSSProperties = {
  width: "100%",
  tableLayout: "fixed",
  borderCollapse: "separate",
  borderSpacing: 0,
};

export const TABLE_DATA_CLASS = "app-data-table";

/** Truncado con ellipsis (celdas de una sola línea). */
export const TRUNCATE_CELL: CSSProperties = {
  overflow: "hidden",
  textOverflow: "ellipsis",
  whiteSpace: "nowrap",
  maxWidth: 0,
};

/** Truncado priorizando contenido visible (sin maxWidth:0 agresivo). */
export const TRUNCATE_CELL_PRIORITY: CSSProperties = {
  overflow: "hidden",
  textOverflow: "ellipsis",
  whiteSpace: "nowrap",
};

/** Texto multilínea completo (nombres largos, etc.). */
export const WRAP_CELL: CSSProperties = {
  whiteSpace: "normal",
  overflowWrap: "anywhere",
  wordBreak: "break-word",
  lineHeight: 1.4,
};

/** Correos / IDs largos: rompe sin solaparse con la columna siguiente. */
export const EMAIL_CELL: CSSProperties = {
  whiteSpace: "normal",
  overflowWrap: "anywhere",
  wordBreak: "break-all",
  lineHeight: 1.4,
};

/** Fecha/hora apilada: evita overflow de "dd/mm/yyyy hh:mm:ss" sobre Acción. */
export const DATE_CELL: CSSProperties = {
  whiteSpace: "normal",
  overflow: "hidden",
  lineHeight: 1.3,
};

export function cellTitle(value: string | null | undefined): string | undefined {
  if (value == null) return undefined;
  const trimmed = String(value).trim();
  if (!trimmed || trimmed === "—") return undefined;
  return trimmed;
}

export const TABLE_SCROLL_WRAP: CSSProperties = {
  overflowX: "auto",
  width: "100%",
};
