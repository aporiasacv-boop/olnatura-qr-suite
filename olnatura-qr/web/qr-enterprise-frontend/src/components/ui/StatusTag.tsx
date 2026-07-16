import { makeStyles, shorthands } from "@fluentui/react-components";

/** Fondos tenues: se distinguen amarillo / verde / rojo sin saturar. */
const useStyles = makeStyles({
  tag: {
    ...shorthands.padding("4px", "10px"),
    borderRadius: "6px",
    fontSize: "13px",
    fontWeight: 600,
    display: "inline-block",
  },
  cuarentena: {
    backgroundColor: "#FFF8E1",
    color: "#8A6D1D",
    ...shorthands.border("1px", "solid", "#F0E0A0"),
  },
  aprobado: {
    backgroundColor: "#EAF6EE",
    color: "#1B5E35",
    ...shorthands.border("1px", "solid", "#B7DFC4"),
  },
  rechazado: {
    backgroundColor: "#FDECEC",
    color: "#8B1E1E",
    ...shorthands.border("1px", "solid", "#F0BABA"),
  },
  other: {
    backgroundColor: "#F3F4F6",
    color: "#4B5563",
    ...shorthands.border("1px", "solid", "#E5E7EB"),
  },
});

export default function StatusTag({ status }: { status: string }) {
  const s = useStyles();
  const raw = (status ?? "").toUpperCase();
  const normalized =
    raw === "PENDIENTE" || raw === "PENDING" || raw === "LIBERADO" || raw === "DESCONOCIDO" || raw === "OPEN"
      ? "CUARENTENA"
      : raw;

  const cls =
    normalized === "APROBADO"
      ? s.aprobado
      : normalized === "RECHAZADO"
        ? s.rechazado
        : normalized === "CUARENTENA"
          ? s.cuarentena
          : s.other;

  const label =
    normalized === "CUARENTENA"
      ? "CUARENTENA"
      : normalized === "APROBADO"
        ? "APROBADO"
        : normalized === "RECHAZADO"
          ? "RECHAZADO"
          : status || "—";

  return <span className={`${s.tag} ${cls}`}>{label}</span>;
}
