import React, { useEffect, useMemo, useState } from "react";
import {
  Button,
  Card,
  Dropdown,
  Input,
  Option,
  Text,
  Tooltip,
  Dialog,
  DialogSurface,
  DialogBody,
  DialogTitle,
  DialogContent,
  DialogActions,
  Link,
} from "@fluentui/react-components";
import { API_BASE, api, ApiError } from "../api/client";
import type { QrResponse, ScanEvent } from "../api/types";
import { useAuth } from "../auth/AuthContext";
import { useToasts } from "../components/ui/toasts";
import LoadingState from "../components/ui/LoadingState";
import EmptyState from "../components/ui/EmptyState";
import ErrorState from "../components/ui/ErrorState";
import StatusTag from "../components/ui/StatusTag";

// Helpers
function getDeviceId() {
  const k = "qr_device_id";
  const existing = localStorage.getItem(k);
  if (existing) return existing;
  const id = `WEB-${crypto.randomUUID()}`;
  localStorage.setItem(k, id);
  return id;
}

function asText(v: any, fallback = "—") {
  if (v === null || v === undefined) return fallback;
  if (typeof v === "string") return v.trim() ? v : fallback;
  if (typeof v === "number" || typeof v === "boolean") return String(v);
  return fallback;
}

function readLabel(data: QrResponse | null, key: string, fallback = "—") {
  return asText((data as any)?.label?.[key], fallback);
}

function readDynamic(data: QrResponse | null, key: string, fallback = "—") {
  return asText((data as any)?.dynamic?.[key], fallback);
}

async function downloadZpl(data: QrResponse | null, loteInput: string) {
  const label: any = (data as any)?.label ?? {};
  const loteFromLabel = typeof label.lote === "string" ? label.lote.trim() : "";
  const lote = loteFromLabel || loteInput.trim();
  if (!lote) return;

  const base = API_BASE.replace(/\/+$/, "");
  const url = `${base}/api/v1/label/${encodeURIComponent(lote)}/zpl`;

  try {
    const res = await fetch(url, { method: "GET", credentials: "include" });
    if (!res.ok) {
      // Best-effort: let caller surface a toast if needed
      console.error("ZPL download failed", res.status, await res.text());
      return;
    }
    const text = await res.text();
    const blob = new Blob([text], { type: "text/plain" });
    const href = URL.createObjectURL(blob);
    const a = document.createElement("a");
    const safeLote = (lote || "label").replace(/[\\s/\\\\]+/g, "_");
    a.href = href;
    a.download = `label-${safeLote}.zpl`;
    document.body.appendChild(a);
    a.click();
    a.remove();
    URL.revokeObjectURL(href);
  } catch (err) {
    console.error("ZPL download error", err);
  }
}

// Status mapping: display label (Spanish) <-> backend value (English)
// PATCH requests must send backend values.
const STATUS_OPTIONS = [
  { value: "PENDING", label: "PENDIENTE" },
  { value: "LIBERADO", label: "LIBERADO" },
  { value: "APROBADO", label: "APROBADO" },
  { value: "CUARENTENA", label: "CUARENTENA" },
  { value: "RECHAZADO", label: "RECHAZADO" },
  { value: "DESCONOCIDO", label: "DESCONOCIDO" },
] as const;

function statusToDisplayLabel(backendValue: string): string {
  const opt = STATUS_OPTIONS.find((o) => o.value === (backendValue ?? "").trim().toUpperCase());
  return opt ? opt.label : (backendValue ?? "—");
}

const DEV_METRICS = import.meta.env.DEV;
const STORAGE_KEY = "qr_suite_metrics_v1";

type LoteMetrics = {
  lookupAt: number;
  firstZplAt: number | null;
  zplCount: number;
};

export default function BatchLookupPage() {
  const { can, hasRole } = useAuth();
  const toasts = useToasts();

  const [lote, setLote] = useState("");
  const [data, setData] = useState<QrResponse | null>(null);
  const [scans, setScans] = useState<ScanEvent[] | null>(null);

  const [status, setStatus] = useState<"idle" | "loading" | "error" | "ok">("idle");
  const [err, setErr] = useState<{ title: string; detail?: string } | null>(null);
  const [newStatus, setNewStatus] = useState<string>("");
  const [statusBusy, setStatusBusy] = useState(false);
  const [zplHelpOpen, setZplHelpOpen] = useState(false);

  const [sessionMetrics, setSessionMetrics] = useState<Map<string, LoteMetrics>>(() => {
    if (!DEV_METRICS) return new Map();
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      if (!raw) return new Map();
      const parsed = JSON.parse(raw);
      const arr = Array.isArray(parsed) ? parsed : [];
      const entries = arr
        .filter((e): e is [string, unknown] => Array.isArray(e) && e.length === 2 && typeof e[0] === "string")
        .map(([k, v]) => {
          const o = v && typeof v === "object" ? (v as Record<string, unknown>) : {};
          return [k, {
            lookupAt: typeof o.lookupAt === "number" ? o.lookupAt : 0,
            firstZplAt: typeof o.firstZplAt === "number" ? o.firstZplAt : null,
            zplCount: typeof o.zplCount === "number" ? o.zplCount : 0,
          }] as [string, LoteMetrics];
        });
      return new Map(entries);
    } catch {
      return new Map();
    }
  });

  const loteTrim = useMemo(() => lote.trim(), [lote]);

  useEffect(() => {
    if (!DEV_METRICS) return;
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(Array.from(sessionMetrics.entries())));
    } catch { /* ignore */ }
  }, [sessionMetrics]);

  // Load batch data
  const load = async () => {
    if (!loteTrim) return;

    setStatus("loading");
    setErr(null);
    setData(null);
    setScans(null);

    try {
      const qr = await api<QrResponse>(`/qr/${encodeURIComponent(loteTrim)}`);
      setData(qr);

      const ev = await api<ScanEvent[]>(`/scan/${encodeURIComponent(loteTrim)}`);
      setScans(Array.isArray(ev) ? ev : []);

      setStatus("ok");

      if (DEV_METRICS) {
        const lookupAt = Date.now();
        setSessionMetrics((prev) => {
          const next = new Map(prev);
          next.set(loteTrim, { lookupAt, firstZplAt: null, zplCount: 0 });
          return next;
        });
        console.log("[ZPL Metrics] lookup recorded", { lote: loteTrim, lookupAt: new Date(lookupAt).toISOString() });
      }
    } catch (e) {
      const ae = e as ApiError;

      // ✅ Aquí NO hacemos toast para no duplicar UX.
      setErr({
        title: ae.status === 404 ? "Lote no encontrado" : "Error al consultar",
        detail:
          ae.status === 404
            ? "Verifica el identificador e intenta de nuevo."
            : ae.status === 401
              ? "Tu sesión expiró. Vuelve a iniciar sesión."
              : "No se pudo obtener la información del lote.",
      });

      setStatus("error");
    }
  };

  const changeStatus = async () => {
    if (!loteTrim || !newStatus || statusBusy) return;
    setStatusBusy(true);
    try {
      await api<void>(`/label/by-lote/${encodeURIComponent(loteTrim)}/status`, {
        method: "PATCH",
        body: { status: newStatus },
      });
      toasts.push({
        intent: "success",
        title: "Estatus actualizado",
        message: `Nuevo estatus: ${statusToDisplayLabel(newStatus)}`,
      });
      await load();
    } catch (e) {
      const ae = e as ApiError;
      toasts.push({
        intent: "error",
        title: "No se pudo actualizar",
        message: ae?.message ?? "Intenta de nuevo.",
        error: ae,
      });
    } finally {
      setStatusBusy(false);
    }
  };

  // Register scan
  const registerScan = async () => {
    if (!loteTrim) return;

    try {
      await api<void>(`/scan/${encodeURIComponent(loteTrim)}`, {
        method: "POST",
        headers: { "X-Device-Id": getDeviceId() },
      });

      toasts.push({
        intent: "success",
        title: "Escaneo registrado",
        message: "Se agregó un nuevo evento para este lote.",
      });

      const ev = await api<ScanEvent[]>(`/scan/${encodeURIComponent(loteTrim)}`);
      setScans(Array.isArray(ev) ? ev : []);
    } catch (e) {
      const ae = e as ApiError;

      toasts.push({
        intent: "error",
        title: "No se pudo registrar el escaneo",
        message:
          ae.status === 401
            ? "Tu sesión expiró o no tienes permisos."
            : "Intenta de nuevo.",
        error: ae,
      });

      setErr({
        title: "No se pudo registrar el escaneo",
        detail:
          ae.status === 401
            ? "Tu sesión expiró o no tienes permisos."
            : "Intenta de nuevo.",
      });

      setStatus("error");
    }
  };

  // Derived values
  const labelEnvase = `${readLabel(data, "envaseNum")} / ${readLabel(data, "envaseTotal")}`;

  const dynamicCantidad = (() => {
    const cant = readDynamic(data, "cantidad");
    const uom = readDynamic(data, "uom", "");
    return uom && uom !== "—" ? `${cant} ${uom}` : cant;
  })();

  const dynamicStatus = (data as any)?.dynamic?.status ?? "—";
  const canChangeStatus = data?.permissions?.canChangeStatus ?? false;
  const canRegisterScan = data?.permissions?.canRegisterScan ?? can("SCAN");
  const canDownloadZpl = data?.permissions?.canCreateLabel ?? (hasRole("ADMIN") || hasRole("ALMACEN"));
  const transitions = data?.availableTransitions ?? [];
  const dropdownOptions = transitions.length > 0
    ? STATUS_OPTIONS.filter((o) => transitions.includes(o.value))
    : [...STATUS_OPTIONS];

  const dynamicFuenteRaw = (data as any)?.dynamic?.fuente ?? "";
  const fuenteNormalized =
    typeof dynamicFuenteRaw === "string" ? dynamicFuenteRaw.trim().toUpperCase() : "";
  const fuenteTooltip =
    fuenteNormalized === "MOCK_DYNAMICS"
      ? "Datos demo para pruebas."
      : fuenteNormalized === "DB_ONLY"
      ? "Status desde base local (sin Dynamics)."
      : undefined;

  const handleZplDownload = async () => {
    if (!loteTrim) return;

    if (DEV_METRICS) {
      setSessionMetrics((prev) => {
        const next = new Map(prev);
        const cur = next.get(loteTrim) ?? { lookupAt: 0, firstZplAt: null, zplCount: 0 };
        const isFirst = cur.firstZplAt === null;
        const firstZplAt = cur.firstZplAt ?? Date.now();
        const newCount = cur.zplCount + 1;
        next.set(loteTrim, { ...cur, firstZplAt, zplCount: newCount });
        if (isFirst) console.log("[ZPL Metrics] first ZPL click", { lote: loteTrim, firstZplAt: new Date(firstZplAt).toISOString() });
        else if (newCount > 1) console.log("[ZPL Metrics] reprint detected", { lote: loteTrim, zplDownloadCount: newCount });
        return next;
      });
    }

    await downloadZpl(data, loteTrim);
  };

  const clearSessionMetrics = () => {
    setSessionMetrics(new Map());
    try { localStorage.removeItem(STORAGE_KEY); } catch { /* ignore */ }
  };

  const exportSessionMetrics = () => {
    const arr = Array.from(sessionMetrics.entries()).map(([loteKey, m]) => ({
      lote: loteKey,
      lookupAt: m.lookupAt ? new Date(m.lookupAt).toISOString() : null,
      firstZplAt: m.firstZplAt ? new Date(m.firstZplAt).toISOString() : null,
      zplCount: m.zplCount,
    }));
    const blob = new Blob([JSON.stringify(arr, null, 2)], { type: "application/json" });
    const href = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = href;
    a.download = "qr_metrics_session.json";
    document.body.appendChild(a);
    a.click();
    a.remove();
    URL.revokeObjectURL(href);
  };

  const handleCopy = async (label: string, value: string) => {
    const v = (value ?? "").toString().trim();
    if (!v) return;
    try {
      await navigator.clipboard.writeText(v);
      toasts.push({
        intent: "success",
        title: "Copiado",
        message: `${label} copiado al portapapeles.`,
      });
    } catch {
      toasts.push({
        intent: "error",
        title: "No se pudo copiar",
        message: "Intenta de nuevo o copia manualmente.",
      });
    }
  };

  // Render
  return (
    <div style={{ display: "grid", gap: 14 }}>
      <div>
        <Text weight="semibold" size={700}>Consulta por lote</Text>
        <div style={{ color: "#6B6B6B", marginTop: 4 }}>
          Consulta por lote. Visualiza datos fijos y estado dinámico.
        </div>
      </div>

      <Card style={{ padding: 16, display: "flex", gap: 10, alignItems: "end" }}>
        <div style={{ flex: 1, display: "grid", gap: 6 }}>
          <Text>Lote</Text>
          <Input
            id="lote"
            name="lote"
            type="text"
            value={lote}
            onChange={(_, d) => setLote(d.value ?? "")}
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

        {canRegisterScan && (
          <Button
            appearance="secondary"
            onClick={() => void registerScan()}
            disabled={!loteTrim || status === "loading"}
            title={!loteTrim ? "Ingresa un lote primero" : undefined}
          >
            Registrar escaneo
          </Button>
        )}
      </Card>

      {status === "loading" && <LoadingState label="Consultando lote…" />}

      {status === "error" && err && (
        <ErrorState title={err.title} detail={err.detail} onRetry={() => void load()} />
      )}

      {status === "ok" && data && (
        <div style={{ display: "grid", gridTemplateColumns: "1.2fr 0.8fr", gap: 14 }}>
          <Card style={{ padding: 16 }}>
            <Text weight="semibold">Datos fijos (Etiqueta)</Text>

            <div style={{ marginTop: 12, display: "grid", gridTemplateColumns: "1fr 1fr", gap: 10 }}>
              <Field label="Tipo material" value={readLabel(data, "tipoMaterial")} />
              <Field label="Nombre" value={readLabel(data, "nombre")} />
              <CopyField
                label="Código"
                value={readLabel(data, "codigo")}
                onCopy={handleCopy}
              />
              <CopyField
                label="Lote"
                value={readLabel(data, "lote")}
                onCopy={handleCopy}
              />
              <Field label="Fecha entrada" value={readLabel(data, "fechaEntrada")} />
              <Field label="Caducidad" value={readLabel(data, "caducidad")} />
              <Field label="Reanálisis" value={readLabel(data, "reanalisis")} />
              <Field label="Envase" value={labelEnvase} />
            </div>

            <div style={{ marginTop: 14, color: "#6B6B6B", fontSize: 12 }}>
              Descargar etiqueta (PNG): disponible desde la pantalla de registrar etiqueta o generar etiqueta.
            </div>

            {canDownloadZpl && (
              <div style={{ marginTop: 10 }}>
                <Button
                  appearance="secondary"
                  size="small"
                  onClick={() => void handleZplDownload()}
                >
                  Descargar ZPL
                </Button>
                <Text style={{ display: "block", marginTop: 4, color: "#6B6B6B", fontSize: 12 }}>
                  Archivo ZPL para impresora Zebra. No se abre como documento.{" "}
                  <Link onClick={() => setZplHelpOpen(true)}>Cómo imprimir</Link>
                </Text>
              </div>
            )}
          </Card>

          <div style={{ display: "grid", gap: 14 }}>
            <Card style={{ padding: 16 }}>
              <Text weight="semibold">Estado dinámico</Text>

              <div style={{ marginTop: 12, display: "grid", gap: 10 }}>
                <div style={{ display: "flex", alignItems: "center", gap: 10, flexWrap: "wrap" }}>
                  <Text>Estado</Text>
                  <StatusTag status={statusToDisplayLabel(dynamicStatus)} />
                  {dropdownOptions.length > 0 ? (
                    <>
                      <Dropdown
                        placeholder="Cambiar a…"
                        selectedOptions={newStatus ? [newStatus] : []}
                        onOptionSelect={(_, d) =>
                          setNewStatus((d.optionValue ?? "") as string)
                        }
                        disabled={!canChangeStatus}
                        style={{ minWidth: 140 }}
                      >
                        {dropdownOptions.map((o) => (
                          <Option key={o.value} value={o.value}>
                            {o.label}
                          </Option>
                        ))}
                      </Dropdown>
                      <Button
                        appearance="primary"
                        size="small"
                        onClick={() => void changeStatus()}
                        disabled={!canChangeStatus || !newStatus || statusBusy}
                      >
                        {statusBusy ? "…" : "Aplicar"}
                      </Button>
                    </>
                  ) : (
                    !canChangeStatus ? null : (
                      <Text style={{ color: "#6B6B6B", fontSize: 12 }}>
                        Sin opciones disponibles
                      </Text>
                    )
                  )}
                </div>

                <Field label="Ubicación" value={readDynamic(data, "ubicacion")} />
                <Field label="Cantidad" value={dynamicCantidad} />
                <Field
                  label="Fuente"
                  value={readDynamic(data, "fuente")}
                  tooltip={fuenteTooltip}
                />
              </div>
            </Card>

            <Card style={{ padding: 16 }}>
              <Text weight="semibold">Historial de escaneos</Text>
              <div style={{ marginTop: 12 }}>
                {scans === null ? null : scans.length === 0 ? (
                  <EmptyState title="Sin escaneos" hint="Este lote aún no registra eventos." />
                ) : (
                  <ScansTable events={scans} />
                )}
              </div>
            </Card>
          </div>
        </div>
      )}

      {status === "idle" && (
        <EmptyState title="Listo para consultar" hint="Ingresa un lote y presiona Buscar." />
      )}

      {DEV_METRICS && loteTrim && (() => {
        const m = sessionMetrics.get(loteTrim);
        const fmt = (ts: number) => new Date(ts).toLocaleTimeString("en-GB", { hour: "2-digit", minute: "2-digit", second: "2-digit", hour12: false });
        const timeToFirstLabelSeconds = m?.firstZplAt && m?.lookupAt ? Math.round((m.firstZplAt - m.lookupAt) / 1000) : null;
        const reprints = m ? Math.max(0, m.zplCount - 1) : 0;
        return (
          <Card style={{ padding: 12, background: "#F8F9FA", border: "1px dashed #CCC" }}>
            <Text weight="semibold" size={500}>Session metrics (dev)</Text>
            {m ? (
              <div style={{ marginTop: 8, display: "flex", flexDirection: "column", gap: 4, fontSize: 13 }}>
                <div>Lookup: {fmt(m.lookupAt)}</div>
                <div>First label: {m.firstZplAt ? fmt(m.firstZplAt) : "—"}</div>
                <div>Time to label: {timeToFirstLabelSeconds !== null ? `${timeToFirstLabelSeconds} s` : "—"}</div>
                <div>ZPL downloads: {m.zplCount}</div>
                <div>Reprints: {reprints}</div>
              </div>
            ) : (
              <Text style={{ marginTop: 8, color: "#6B6B6B", fontSize: 12 }}>No metrics yet. Perform a lookup and download ZPL.</Text>
            )}
            <div style={{ marginTop: 10, display: "flex", gap: 8 }}>
              <Button appearance="subtle" size="small" onClick={clearSessionMetrics}>
                Clear session metrics
              </Button>
              <Button appearance="subtle" size="small" onClick={exportSessionMetrics}>
                Export metrics (JSON)
              </Button>
            </div>
          </Card>
        );
      })()}

      <Dialog open={zplHelpOpen} onOpenChange={(_, data) => setZplHelpOpen(data.open)}>
        <DialogSurface>
          <DialogBody>
            <DialogTitle>Cómo imprimir archivos ZPL</DialogTitle>
            <DialogContent>
              <ul style={{ paddingLeft: 18, margin: "8px 0 0 0" }}>
                <li>Descarga el archivo .zpl y guárdalo en tu equipo.</li>
                <li>
                  En equipos con impresora Zebra, envía el archivo al puerto de la impresora
                  (por ejemplo, arrastrando el archivo a la impresora o usando utilidades de Zebra).
                </li>
                <li>
                  No intentes abrir el archivo como documento; es código de comandos para la
                  impresora.
                </li>
              </ul>
            </DialogContent>
            <DialogActions>
              <Button appearance="primary" onClick={() => setZplHelpOpen(false)}>
                Cerrar
              </Button>
            </DialogActions>
          </DialogBody>
        </DialogSurface>
      </Dialog>
    </div>
  );
}

// Sub-components
function Field({
  label,
  value,
  tooltip,
}: {
  label: string;
  value: React.ReactNode;
  tooltip?: string;
}) {
  const labelNode = tooltip ? (
    <Tooltip content={tooltip} relationship="label">
      <span>{label}</span>
    </Tooltip>
  ) : (
    label
  );

  return (
    <div style={{ border: "1px solid #E6E6E6", borderRadius: 10, padding: 10 }}>
      <div style={{ color: "#6B6B6B", fontSize: 12 }}>{labelNode}</div>
      <div style={{ marginTop: 4, fontWeight: 600 }}>{value}</div>
    </div>
  );
}

function CopyField({
  label,
  value,
  onCopy,
}: {
  label: string;
  value: string;
  onCopy: (label: string, value: string) => void;
}) {
  const display = value ?? "—";
  return (
    <div style={{ border: "1px solid #E6E6E6", borderRadius: 10, padding: 10 }}>
      <div
        style={{
          display: "flex",
          alignItems: "center",
          justifyContent: "space-between",
          gap: 8,
          color: "#6B6B6B",
          fontSize: 12,
        }}
      >
        <span>{label}</span>
        <Button
          appearance="subtle"
          size="small"
          onClick={() => onCopy(label, display)}
        >
          Copiar
        </Button>
      </div>
      <div style={{ marginTop: 4, fontWeight: 600, wordBreak: "break-all" }}>{display}</div>
    </div>
  );
}

function pick(ev: any, keys: string[], fallback = "—") {
  for (const k of keys) {
    const v = ev?.[k];
    if (typeof v === "string" && v.trim()) return v;
    if (typeof v === "number") return String(v);
  }
  return fallback;
}

function ScansTable({ events }: { events: any[] }) {
  return (
    <div style={{ border: "1px solid #E6E6E6", borderRadius: 12, overflow: "hidden" }}>
      <div
        style={{
          display: "grid",
          gridTemplateColumns: "1.3fr 1fr 1fr 1fr",
          padding: "10px 12px",
          background: "#F6F7F8",
          fontWeight: 600,
        }}
      >
        <div>Fecha</div>
        <div>Dispositivo</div>
        <div>Usuario</div>
        <div>Ubicación</div>
      </div>

      {events.map((ev, idx) => (
        <div
          key={idx}
          style={{
            display: "grid",
            gridTemplateColumns: "1.3fr 1fr 1fr 1fr",
            padding: "10px 12px",
            borderTop: "1px solid #EFEFEF",
          }}
        >
          <div>{pick(ev, ["fecha", "timestamp", "createdAt"])}</div>
          <div>{pick(ev, ["dispositivo", "device", "deviceId"])}</div>
          <div>{pick(ev, ["usuario", "user", "username"])}</div>
          <div>{pick(ev, ["ubicacion", "location"])}</div>
        </div>
      ))}
    </div>
  );
}