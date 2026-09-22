import { useMemo, useState } from "react";
import { Button, Text, makeStyles, shorthands } from "@fluentui/react-components";
import { api, ApiError } from "../api/client";
import AppCard from "../components/ui/AppCard";
import { useToasts } from "../components/ui/toasts";
import { brand } from "../styles/brand";

const CONFIRM_TOKEN = "PURGE_LOTS";

type PurgeResponse = {
  loteCommentsDeleted: number;
  scanEventsDeleted: number;
  auditEventsDeleted: number;
  qrLabelsDeleted: number;
};

const useStyles = makeStyles({
  wrap: {
    display: "grid",
    gap: "16px",
    maxWidth: "720px",
  },
  title: {
    fontSize: "20px",
    fontWeight: 700,
    color: brand.text,
    margin: 0,
  },
  subtitle: {
    fontSize: "14px",
    color: brand.muted,
    marginTop: "4px",
    lineHeight: 1.45,
  },
  sectionTitle: {
    fontSize: "16px",
    fontWeight: 600,
    color: brand.text,
    margin: "0 0 8px",
  },
  body: {
    display: "grid",
    gap: "12px",
  },
  warning: {
    ...shorthands.padding("12px"),
    ...shorthands.borderRadius("10px"),
    ...shorthands.border("1px", "solid", "#F0BABA"),
    backgroundColor: "#FDECEC",
    color: "#8B1E1E",
    fontSize: "13px",
    lineHeight: 1.45,
  },
  actions: {
    display: "flex",
    flexWrap: "wrap",
    gap: "10px",
    alignItems: "center",
  },
  result: {
    fontSize: "13px",
    color: brand.text2,
    lineHeight: 1.5,
  },
  muted: {
    color: brand.muted,
    fontSize: "13px",
  },
});

export default function AdminDbPage() {
  const s = useStyles();
  const { push } = useToasts();
  const [busy, setBusy] = useState(false);
  const [lastResult, setLastResult] = useState<PurgeResponse | null>(null);

  const statusText = useMemo(() => {
    if (busy) return "Eliminando lotes…";
    return "Acción irreversible. Los usuarios no se tocan.";
  }, [busy]);

  async function handlePurgeLots() {
    if (busy) return;

    const first = window.confirm(
      "¿Eliminar todos los lotes de la BD?\n\nSe borrarán etiquetas QR y, para no dejar basura, también comentarios, escaneos y auditoría ligada a lotes.\nLos usuarios no se eliminan.\n\nEsta acción es irreversible."
    );
    if (!first) return;

    const second = window.confirm(
      "Confirmación final.\n\n¿Seguro que quieres eliminar todos los lotes ahora?"
    );
    if (!second) return;

    setBusy(true);
    try {
      const res = await api<PurgeResponse>("/temp/admin/purge-operational-data", {
        method: "POST",
        body: { confirm: CONFIRM_TOKEN },
        toast: false,
      });
      setLastResult(res);
      push({
        intent: "success",
        title: "Lotes eliminados",
        message: `Etiquetas ${res.qrLabelsDeleted}, escaneos ${res.scanEventsDeleted}, comentarios ${res.loteCommentsDeleted}, auditoría ${res.auditEventsDeleted}.`,
      });
    } catch (err) {
      const ae = err as ApiError;
      push({
        intent: "error",
        title: "No se pudieron eliminar los lotes",
        message: ae?.message ?? "Revisa permisos o intenta de nuevo.",
        error: ae,
      });
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className={s.wrap}>
      <div>
        <h1 className={s.title}>Administración de BD</h1>
        <p className={s.subtitle}>
          Mantenimiento de datos operativos. Solo administradores.
        </p>
      </div>

      <AppCard>
        <h2 className={s.sectionTitle}>Eliminar lotes</h2>
        <div className={s.body}>
          <div className={s.warning}>
            Elimina todas las etiquetas/lotes de la BD. También limpia comentarios, escaneos y
            auditoría asociada a lotes. No elimina usuarios. Se puede volver a ejecutar.
          </div>

          <div className={s.actions}>
            <Button
              appearance="primary"
              disabled={busy}
              onClick={() => void handlePurgeLots()}
            >
              {busy ? "Eliminando…" : "Eliminar lotes de la BD"}
            </Button>
            <Text className={s.muted}>{statusText}</Text>
          </div>

          {lastResult ? (
            <Text className={s.result}>
              Resultado: {lastResult.qrLabelsDeleted} etiquetas,{" "}
              {lastResult.scanEventsDeleted} escaneos,{" "}
              {lastResult.loteCommentsDeleted} comentarios,{" "}
              {lastResult.auditEventsDeleted} auditoría.
            </Text>
          ) : null}
        </div>
      </AppCard>
    </div>
  );
}
