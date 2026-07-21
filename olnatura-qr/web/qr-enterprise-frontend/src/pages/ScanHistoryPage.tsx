import { useMemo, useState } from "react";
import { Button, Text } from "@fluentui/react-components";
import AppCard from "../components/ui/AppCard";
import { brand } from "../styles/brand";
import { api, ApiError } from "../api/client";
import type { ScanEvent } from "../api/types";
import { useToasts } from "../components/ui/toasts";
import LoadingState from "../components/ui/LoadingState";
import EmptyState from "../components/ui/EmptyState";
import ErrorState from "../components/ui/ErrorState";
import ScanHistoryTable from "../components/ui/ScanHistoryTable";
import LoteAutocomplete from "../components/ui/LoteAutocomplete";
import { LABELS } from "../utils/displayLabels";

export default function ScanHistoryPage() {
  const toasts = useToasts();

  const [lote, setLote] = useState("");
  const loteTrim = useMemo(() => lote.trim(), [lote]);

  const [status, setStatus] = useState<"idle" | "loading" | "ok" | "error">("idle");
  const [events, setEvents] = useState<ScanEvent[] | null>(null);
  const [err, setErr] = useState<{ title: string; detail?: string } | null>(null);

  const load = async (loteOverride?: string) => {
    const key = (loteOverride ?? lote).trim();
    if (!key) return;

    setStatus("loading");
    setErr(null);
    setEvents(null);

    try {
      const ev = await api<ScanEvent[]>(`/scan/${encodeURIComponent(key)}`);
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
    <div style={{ display: "grid", gap: 24 }}>
      <h1 style={{ fontSize: "20px", fontWeight: 600, color: brand.text, margin: 0 }}>{LABELS.scanHistory}</h1>

      <AppCard style={{ display: "flex", gap: 12, alignItems: "flex-end" }}>
        <form
          style={{ display: "contents" }}
          onSubmit={(e) => {
            e.preventDefault();
            void load();
          }}
        >
          <div style={{ flex: 1, display: "grid", gap: 6 }}>
            <Text>Lote</Text>
            <LoteAutocomplete
              id="lote"
              name="lote"
              value={lote}
              onChange={setLote}
              onSelect={(item) => {
                setLote(item.lote);
                void load(item.lote);
              }}
              placeholder="Ej. 251201-MEM0003454"
            />
          </div>
          <Button
            appearance="primary"
            type="submit"
            disabled={!loteTrim || status === "loading"}
          >
            Buscar
          </Button>
        </form>
      </AppCard>

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
          <EmptyState title={LABELS.noEvents} />
        ) : (
          <AppCard>
            <Text weight="semibold">{LABELS.scanHistory}</Text>
            <div style={{ marginTop: 12 }}>
              <ScanHistoryTable events={events} />
            </div>
          </AppCard>
        )
      )}
    </div>
  );
}
