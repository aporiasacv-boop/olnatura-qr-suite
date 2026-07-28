import { makeStyles, shorthands } from "@fluentui/react-components";

/** Fondos tenues: se distinguen amarillo / verde / rojo / gris sin saturar. */
const useStyles = makeStyles({
  tag: {
    ...shorthands.padding("4px", "10px"),
    borderRadius: "6px",
    fontSize: "13px",
    fontWeight: 600,
    display: "inline-flex",
    alignItems: "center",
    gap: "6px",
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
  desconocido: {
    backgroundColor: "#F3F4F6",
    color: "#4B5563",
    ...shorthands.border("1px", "solid", "#E5E7EB"),
  },
});

export type OperationalStatusKind = "APROBADO" | "CUARENTENA" | "RECHAZADO" | "DESCONOCIDO";

export function normalizeOperationalStatus(status: string | null | undefined): OperationalStatusKind {
  const raw = (status ?? "").trim().toUpperCase();
  if (raw === "APROBADO") return "APROBADO";
  if (raw === "RECHAZADO") return "RECHAZADO";
  if (raw === "CUARENTENA") return "CUARENTENA";
  return "DESCONOCIDO";
}

export function operationalStatusDisplayLabel(status: string | null | undefined): string {
  const n = normalizeOperationalStatus(status);
  if (n === "APROBADO") return "Aprobado";
  if (n === "CUARENTENA") return "Cuarentena";
  if (n === "RECHAZADO") return "Rechazado";
  return "No determinado";
}

export function operationalStatusEmoji(status: string | null | undefined): string {
  const n = normalizeOperationalStatus(status);
  if (n === "APROBADO") return "🟢";
  if (n === "CUARENTENA") return "🟡";
  if (n === "RECHAZADO") return "🔴";
  return "⚪";
}

export default function StatusTag({ status }: { status: string }) {
  const s = useStyles();
  const normalized = normalizeOperationalStatus(status);

  const cls =
    normalized === "APROBADO"
      ? s.aprobado
      : normalized === "RECHAZADO"
        ? s.rechazado
        : normalized === "CUARENTENA"
          ? s.cuarentena
          : s.desconocido;

  const label = `${operationalStatusEmoji(normalized)} ${operationalStatusDisplayLabel(normalized)}`;

  return <span className={`${s.tag} ${cls}`}>{label}</span>;
}
