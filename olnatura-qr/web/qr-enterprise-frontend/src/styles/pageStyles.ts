import { makeStyles, shorthands } from "@fluentui/react-components";
import { brand } from "./brand";

export const usePageStyles = makeStyles({
  page: {
    display: "grid",
    gap: "24px",
    maxWidth: "1200px",
  },
  title: {
    fontSize: "20px",
    fontWeight: 600,
    color: brand.text,
    margin: 0,
  },
  row: {
    display: "grid",
    gap: "8px",
  },
  formRow: {
    display: "grid",
    gap: "12px",
  },
  actions: {
    display: "flex",
    gap: "10px",
    flexWrap: "wrap",
  },
  inputBase: {
    fontSize: "14px",
    minHeight: "40px",
    ...shorthands.padding("0", "12px"),
    borderRadius: "8px",
  },
  card: {
    backgroundColor: brand.surface,
    borderRadius: "12px",
    boxShadow: "0 1px 3px rgba(0,0,0,0.06)",
    ...shorthands.border("1px", "solid", brand.border),
    ...shorthands.padding("16px"),
  },
  label: {
    fontSize: "14px",
    fontWeight: 500,
    color: brand.text2,
  },
  muted: {
    fontSize: "12px",
    color: brand.muted,
  },
});
