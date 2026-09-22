import type { CSSProperties } from "react";

export const TABLE_FIXED_STYLE: CSSProperties = {
  width: "100%",
  maxWidth: "100%",
  tableLayout: "fixed",
  borderCollapse: "separate",
  borderSpacing: 0,
};

export const TABLE_DATA_CLASS = "app-data-table";

export const WRAP_CELL: CSSProperties = {
  whiteSpace: "normal",
  overflowWrap: "anywhere",
  wordBreak: "break-word",
  lineHeight: 1.4,
  overflow: "visible",
};

export const EMAIL_CELL: CSSProperties = {
  whiteSpace: "normal",
  overflowWrap: "anywhere",
  wordBreak: "break-all",
  lineHeight: 1.4,
  overflow: "visible",
};

export const DATE_CELL: CSSProperties = {
  whiteSpace: "normal",
  overflowWrap: "anywhere",
  wordBreak: "break-word",
  lineHeight: 1.35,
  overflow: "visible",
};

export const TRUNCATE_CELL: CSSProperties = WRAP_CELL;
export const TRUNCATE_CELL_PRIORITY: CSSProperties = WRAP_CELL;

export function cellTitle(value: string | null | undefined): string | undefined {
  if (value == null) return undefined;
  const trimmed = String(value).trim();
  if (!trimmed || trimmed === "—") return undefined;
  return trimmed;
}

export const TABLE_SCROLL_WRAP: CSSProperties = {
  width: "100%",
  maxWidth: "100%",
  minWidth: 0,
  overflowX: "hidden",
  overflowY: "visible",
};
