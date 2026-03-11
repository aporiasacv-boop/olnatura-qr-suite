import { useMemo, useState } from "react";
import { Button, Card, Input, Text } from "@fluentui/react-components";
import { api, ApiError } from "../api/client";
import type { ScanEvent } from "../api/types";
import { useToasts } from "../components/ui/toasts";
import LoadingState from "../components/ui/LoadingState";
import EmptyState from "../components/ui/EmptyState";
import ErrorState from "../components/ui/ErrorState";
import ScanHistoryTable from "../components/ui/ScanHistoryTable";
import { LABELS } from "../utils/displayLabels";

export default function ScanHistoryPage() {
  const toasts = useToasts();

  const [lote, setLote] = useState("");
  const loteTrim = useMemo(() => lote.trim(), [lote]);

  const [status, setStatus] = useState<"idle" | "loading" | "ok" | "error">("idle");
  const [events, setEvents] = useState<ScanEvent[] | null>(null);
  const [err, setErr] = useState<{ title: string; detail?: string } | null>(null);

  const load = async () => {
    if (!loteTrim) return;

    setStatus("loading");
    setErr(null);
    setEvents(null);

    try {
      const ev = await api<ScanEvent[]>(`/scan/${encodeURIComponent(loteTrim)}`);
      setEvents(Array.isArray(ev) ? ev : []);
      setStatus("ok");
    } catch (e) {
      const ae = e as ApiError;

      toasts.push({
        intent: "error",
        title:
          ae.status === 404
            ? LABELS.noEvents
            : "Error al consultar historial",
        message:
          ae.status === 404
            ? LABELS.noRecords
            : ae.status === 401
            ? "Tu sesión no es válida."
            : "Intenta de nuevo.",
        error: ae,
      });

      setErr({
        title:
          ae.status === 404
            ? LABELS.noEvents
            : "Error al consultar historial",
        detail:
          ae.status === 404
            ? LABELS.noRecords
            : ae.status === 401
            ? "Vuelve a iniciar sesión."
            : "Intenta de nuevo.",
      });

      setStatus("error");
    }
  };

  return (
    <div style={{ display: "grid", gap: 14 }}>
      <div>
        <Text weight="semibold" size={700}>
          {LABELS.scanHistory}
        </Text>
        <div style={{ color: "#6B6B6B", marginTop: 4 }}>
          Consulta por lote para revisar trazabilidad.
        </div>
      </div>

      <Card style={{ padding: 16, display: "flex", gap: 10, alignItems: "end" }}>
        <div style={{ flex: 1, display: "grid", gap: 6 }}>
          <Text>Lote</Text>
          <Input
            id="lote"
            name="lote"
            value={lote}
            onChange={(_, d) => setLote(d.value)}
            placeholder="Ej. 251201-MEM0003454"
          />
        </div>
        <Button
          appearance="primary"
          onClick={() => void load()}
          disabled={!loteTrim || status === "loading"}
        >
          Buscar
        </Button>
      </Card>

      {status === "idle" && (
        <EmptyState
          title={LABELS.readyToFilter}
          hint="Ingresa un lote para ver sus eventos."
        />
      )}

      {status === "loading" && (
        <LoadingState label="Consultando historial…" />
      )}

      {status === "error" && err && (
        <ErrorState
          title={err.title}
          detail={err.detail}
          onRetry={() => void load()}
        />
      )}

      {status === "ok" && events && (
        events.length === 0 ? (
          <EmptyState
            title={LABELS.noEvents}
            hint={LABELS.noRecords}
          />
        ) : (
          <Card style={{ padding: 16 }}>
            <Text weight="semibold">{LABELS.scanHistory}</Text>
            <div style={{ marginTop: 12 }}>
              <ScanHistoryTable events={events} />
            </div>
          </Card>
        )
      )}
    </div>
  );
}