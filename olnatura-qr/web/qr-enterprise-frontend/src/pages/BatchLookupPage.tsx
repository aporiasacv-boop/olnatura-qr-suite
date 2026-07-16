import React, { useEffect, useMemo, useState } from "react";
import {
  Button,
  Input,
  Text,
  Tooltip,
  Dialog,
  DialogSurface,
  DialogBody,
  DialogTitle,
  DialogContent,
  DialogActions,
  Link,
  makeStyles,
  shorthands,
} from "@fluentui/react-components";
import AppCard from "../components/ui/AppCard";
import { brand } from "../styles/brand";
import { API_BASE, api, ApiError } from "../api/client";
import type { QrResponse, ScanEvent, ApprovalLeg } from "../api/types";
import { useAuth } from "../auth/AuthContext";
import { useToasts } from "../components/ui/toasts";
import LoadingState from "../components/ui/LoadingState";
import EmptyState from "../components/ui/EmptyState";
import ErrorState from "../components/ui/ErrorState";
import StatusTag from "../components/ui/StatusTag";
import { LABELS, fuenteDisplay, formatDateTime } from "../utils/displayLabels";
import ScanHistoryTable from "../components/ui/ScanHistoryTable";
import LoteAutocomplete from "../components/ui/LoteAutocomplete";

function needsCalidadApproval(tipo: string): boolean {
  const t = (tipo || "").toUpperCase();
  return t.includes("MATERIA_PRIMA") || t.includes("EMPAQUE_PRIMARIO") || t === "MP";
}

function needsInspeccionApproval(tipo: string): boolean {
  const t = (tipo || "").toUpperCase();
  return t.includes("EMPAQUE_PRIMARIO") || t.includes("EMPAQUE_SECUNDARIO");
}

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

async function downloadZpl(
  data: QrResponse | null,
  loteInput: string,
  opts?: { totalEnvases?: number; printFrom?: number; printTo?: number }
) {
  const label: any = (data as any)?.label ?? {};
  const loteFromLabel = typeof label.lote === "string" ? label.lote.trim() : "";
  const lote = loteFromLabel || loteInput.trim();
  if (!lote) return;

  const total = opts?.totalEnvases ?? label.envaseTotal ?? 1;
  const from = opts?.printFrom ?? label.envaseNum ?? 1;
  const to = opts?.printTo ?? from;

  const base = API_BASE.replace(/\/+$/, "");
  const params = new URLSearchParams();
  if (total !== (label.envaseTotal ?? 1)) params.set("total", String(total));
  if (from !== to || from !== (label.envaseNum ?? 1)) {
    params.set("from", String(from));
    params.set("to", String(to));
  }
  const qs = params.toString();
  const url = `${base}/api/v1/label/${encodeURIComponent(lote)}/zpl${qs ? `?${qs}` : ""}`;

  try {
    const res = await fetch(url, { method: "GET", credentials: "include" });
    if (!res.ok) {
      console.error("ZPL download failed", res.status, await res.text());
      return;
    }
    const text = await res.text();
    const blob = new Blob([text], { type: "text/plain" });
    const href = URL.createObjectURL(blob);
    const a = document.createElement("a");
    const cd = res.headers.get("Content-Disposition");
    let filename = `label-${(lote || "label").replace(/[\s/\\]+/g, "_")}.zpl`;
    if (cd) {
      const m = cd.match(/filename="?([^";\n]+)"?/);
      if (m?.[1]) filename = m[1].trim();
    } else if (from !== to) {
      filename = `etiqueta-${(lote || "label").replace(/[\s/\\]+/g, "_")}-del-${from}-al-${to}.zpl`;
    }
    a.href = href;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    a.remove();
    URL.revokeObjectURL(href);
  } catch (err) {
    console.error("ZPL download error", err);
  }
}

async function downloadAuditPdf(
  loteInput: string,
  onError: (msg: string) => void
) {
  const lote = (loteInput ?? "").trim();
  if (!lote) return;

  const base = API_BASE.replace(/\/+$/, "");
  const url = `${base}/api/v1/audit/${encodeURIComponent(lote)}/pdf`;

  try {
    const res = await fetch(url, { method: "GET", credentials: "include" });
    if (!res.ok) {
      onError(res.status === 404 ? "Lote no encontrado." : "No se pudo descargar el PDF.");
      return;
    }
    const blob = await res.blob();
    const href = URL.createObjectURL(blob);
    const a = document.createElement("a");
    const safeLote = (lote || "lote").replace(/[\s/\\]+/g, "_");
    a.href = href;
    a.download = `trazabilidad-${safeLote}.pdf`;
    document.body.appendChild(a);
    a.click();
    a.remove();
    URL.revokeObjectURL(href);
  } catch (err) {
    onError("Error al descargar. Verifica la conexión.");
  }
}

const STATUS_OPTIONS = [
  { value: "CUARENTENA", label: "CUARENTENA" },
  { value: "APROBADO", label: "APROBADO" },
  { value: "RECHAZADO", label: "RECHAZADO" },
] as const;

function statusToDisplayLabel(backendValue: string): string {
  const v = (backendValue ?? "").trim().toUpperCase();
  if (!v || v === "—" || v === "PENDING" || v === "PENDIENTE" || v === "LIBERADO" || v === "DESCONOCIDO" || v === "OPEN") {
    return "CUARENTENA";
  }
  const opt = STATUS_OPTIONS.find((o) => o.value === v);
  return opt ? opt.label : v;
}

const useStyles = makeStyles({
  page: { display: "grid", gap: "24px" },
  title: { fontSize: "20px", fontWeight: 600, color: brand.text },
  searchCard: {
    ...shorthands.padding("16px"),
    display: "flex",
    gap: "12px",
    alignItems: "flex-end",
  },
  searchInput: { flex: 1 },
  resultGrid: {
    display: "grid",
    gridTemplateColumns: "1.2fr 0.8fr",
    gap: "24px",
    alignItems: "start",
  },
  historyFull: { width: "100%" },
  dataGrid: {
    marginTop: "12px",
    display: "grid",
    gridTemplateColumns: "1fr 1fr",
    gap: "10px",
  },
  fieldBox: {
    ...shorthands.border("1px", "solid", brand.border),
    ...shorthands.borderRadius("10px"),
    ...shorthands.padding("10px"),
  },
  fieldLabel: { color: brand.muted, fontSize: "12px" },
  fieldValue: { marginTop: "4px", fontWeight: 600 },
});

export default function BatchLookupPage() {
  const s = useStyles();
  const { can, hasRole } = useAuth();
  const toasts = useToasts();

  const [lote, setLote] = useState("");
  const [data, setData] = useState<QrResponse | null>(null);
  const [scans, setScans] = useState<ScanEvent[] | null>(null);

  const [status, setStatus] = useState<"idle" | "loading" | "error" | "ok">("idle");
  const [err, setErr] = useState<{ title: string; detail?: string } | null>(null);
  const [statusBusy, setStatusBusy] = useState(false);
  const [zplHelpOpen, setZplHelpOpen] = useState(false);
  const [zplTotalEnvases, setZplTotalEnvases] = useState<string>("");
  const [zplPrintFrom, setZplPrintFrom] = useState<string>("");
  const [zplPrintTo, setZplPrintTo] = useState<string>("");

  const loteTrim = useMemo(() => lote.trim(), [lote]);

  const load = async (loteOverride?: string) => {
    const key = (loteOverride ?? lote).trim();
    if (!key) return;

    setStatus("loading");
    setErr(null);
    setData(null);
    setScans(null);

    try {
      const qr = await api<QrResponse>(`/qr/${encodeURIComponent(key)}`);
      setData(qr);

      const ev = await api<ScanEvent[]>(`/scan/${encodeURIComponent(key)}`);
      setScans(Array.isArray(ev) ? ev : []);

      setStatus("ok");

    } catch (e) {
      const ae = e as ApiError;
      const isDynamics =
        ae.status === 502 ||
        ae.status === 504 ||
        (typeof ae.body?.error === "string" && String(ae.body.error).startsWith("DYNAMICS_"));

      setErr({
        title:
          ae.status === 404
            ? "Lote no encontrado"
            : isDynamics
              ? "Error de Dynamics 365"
              : "Error al consultar",
        detail:
          ae.status === 404
            ? "Verifica el identificador e intenta de nuevo."
            : ae.status === 401
              ? "Tu sesión expiró. Vuelve a iniciar sesión."
              : isDynamics
                ? ae.message ||
                  "No se pudo obtener datos de Dynamics 365. Intenta de nuevo o contacta a soporte."
                : ae.message || "No se pudo obtener la información del lote.",
      });

      setStatus("error");
    }
  };

  const changeStatus = async (action: "approve" | "reject") => {
    if (!loteTrim || statusBusy) return;
    setStatusBusy(true);
    try {
      const path =
        action === "approve"
          ? `/label/by-lote/${encodeURIComponent(loteTrim)}/approve`
          : `/label/by-lote/${encodeURIComponent(loteTrim)}/reject`;
      await api<{ id: string; status: string }>(path, { method: "POST", toast: false });
      toasts.push({
        intent: "success",
        title: action === "approve" ? "Aprobación registrada" : "Material rechazado",
        message: action === "approve" ? "Se actualizó el estado del lote." : "El lote quedó en RECHAZADO.",
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

  const labelEnvase = `${readLabel(data, "envaseNum")} / ${readLabel(data, "envaseTotal")}`;
  const envaseTotal = parseInt(String((data as any)?.label?.envaseTotal ?? 1), 10) || 1;

  useEffect(() => {
    if (status === "ok" && data) {
      setZplTotalEnvases(String(envaseTotal));
      setZplPrintFrom("1");
      setZplPrintTo(String(envaseTotal));
    }
  }, [status, data, envaseTotal]);

  const dynamicCantidad = (() => {
    const cant = readDynamic(data, "cantidadAlmacen");
    const qty = cant !== "—" ? cant : readDynamic(data, "cantidad");
    if (qty === "—") return qty;
    const unit = String((data as any)?.dynamic?.unidadInventario ?? "").trim();
    return unit ? `${qty} ${unit}` : qty;
  })();

  const dynamicStatus =
    (data as any)?.dynamic?.status ??
    "—";
  const statusDynamicsRef =
    (data as any)?.dynamic?.statusDynamics ??
    "—";
  const canChangeStatus = data?.permissions?.canChangeStatus ?? false;
  const canApprove =
    !!(data?.permissions?.canApproveCalidad || data?.permissions?.canApproveInspeccion);
  const canReject = !!data?.permissions?.canReject;
  const pendingMessage = data?.permissions?.pendingMessage ?? null;
  const tipoMaterialDisplay = data?.permissions?.tipoMaterialDisplay
    ?? readLabel(data, "tipoMaterial");
  const tipoMaterialCode = String((data as any)?.label?.tipoMaterial ?? "").trim();
  const canRegisterScan = data?.permissions?.canRegisterScan ?? can("SCAN");
  const canDownloadZpl = data?.permissions?.canCreateLabel ?? can("GENERATE_LABEL");
  const canDownloadPdf = data?.permissions?.canDownloadAuditPdf
    ?? (hasRole("ADMIN") || hasRole("CALIDAD") || hasRole("INSPECCION"));
  const calidadApproved = !!data?.permissions?.calidadApproved;
  const inspeccionApproved = !!data?.permissions?.inspeccionApproved;
  const calidadLeg = data?.permissions?.calidad;
  const inspeccionLeg = data?.permissions?.inspeccion;

  const dynamicFuenteRaw = (data as any)?.dynamic?.fuente ?? "";
  const fuenteDisplayLabel = fuenteDisplay(dynamicFuenteRaw);

  const handleZplDownload = async () => {
    if (!loteTrim) return;

    const total = parseInt(zplTotalEnvases, 10) || envaseTotal;
    const from = parseInt(zplPrintFrom, 10) || 1;
    const to = parseInt(zplPrintTo, 10) || total;
    await downloadZpl(data, loteTrim, {
      totalEnvases: total,
      printFrom: Math.max(1, Math.min(from, total)),
      printTo: Math.max(1, Math.min(to, total)),
    });
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

  return (
    <div className={s.page}>
      <h1 className={s.title}>{LABELS.lookup}</h1>

      <AppCard className={s.searchCard}>
        <form
          style={{ display: "contents" }}
          onSubmit={(e) => {
            e.preventDefault();
            void load();
          }}
        >
          <div className={s.searchInput} style={{ display: "grid", gap: 6 }}>
            <Text style={{ fontSize: 14, fontWeight: 500 }}>Lote</Text>
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

        {canRegisterScan && (
          <Button
            appearance="secondary"
            type="button"
            onClick={() => void registerScan()}
            disabled={!loteTrim || status === "loading"}
            title={!loteTrim ? "Ingresa un lote primero" : undefined}
          >
            {LABELS.registerScan}
          </Button>
        )}
      </AppCard>

      {status === "loading" && <LoadingState label="Consultando lote…" />}

      {status === "error" && err && (
        <ErrorState title={err.title} detail={err.detail} onRetry={() => void load()} />
      )}

      {status === "ok" && data && (
        <>
          <div className={s.resultGrid}>
            <AppCard>
              <Text weight="semibold">{LABELS.labelData}</Text>
              <div className={s.dataGrid}>
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
                <Field label={LABELS.envase} value={labelEnvase} />
              </div>

              {canDownloadZpl && (
                <div style={{ marginTop: 10 }}>
                  <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr 1fr", gap: 8, marginBottom: 8 }}>
                    <div>
                      <Text style={{ fontSize: 12, color: brand.muted }}>Total envases</Text>
                      <Input
                        type="number"
                        min={1}
                        value={zplTotalEnvases}
                        onChange={(_, d) => setZplTotalEnvases(d.value ?? "")}
                        placeholder={String(envaseTotal)}
                      />
                    </div>
                    <div>
                      <Text style={{ fontSize: 12, color: brand.muted }}>Imprimir desde</Text>
                      <Input
                        type="number"
                        min={1}
                        value={zplPrintFrom}
                        onChange={(_, d) => setZplPrintFrom(d.value ?? "")}
                        placeholder="1"
                      />
                    </div>
                    <div>
                      <Text style={{ fontSize: 12, color: brand.muted }}>Imprimir hasta</Text>
                      <Input
                        type="number"
                        min={1}
                        value={zplPrintTo}
                        onChange={(_, d) => setZplPrintTo(d.value ?? "")}
                        placeholder={String(envaseTotal)}
                      />
                    </div>
                  </div>
                  <Button
                    appearance="secondary"
                    size="small"
                    onClick={() => void handleZplDownload()}
                  >
                    {LABELS.downloadZpl}
                  </Button>
                  <Text style={{ display: "block", marginTop: 4, color: brand.muted, fontSize: 12 }}>
                    Archivo para impresora Zebra.{" "}
                    <Link onClick={() => setZplHelpOpen(true)}>Cómo imprimir</Link>
                  </Text>
                </div>
              )}
            </AppCard>

            <AppCard>
              <Text weight="semibold">{LABELS.dynamicState}</Text>

              <div style={{ marginTop: 12, display: "grid", gap: 10 }}>
                <div style={{ display: "flex", alignItems: "center", gap: 10, flexWrap: "wrap" }}>
                  <Text>{LABELS.dynamicStatus}</Text>
                  <StatusTag status={statusToDisplayLabel(dynamicStatus)} />
                </div>

                <Field label="Tipo de material" value={tipoMaterialDisplay} />

                {(needsCalidadApproval(tipoMaterialCode) || needsInspeccionApproval(tipoMaterialCode)) && (
                  <div style={{ fontSize: 13, color: brand.text2, display: "grid", gap: 8 }}>
                    {needsCalidadApproval(tipoMaterialCode) ? (
                      <ApprovalLegBlock
                        title="Calidad"
                        approved={calidadApproved}
                        leg={calidadLeg}
                      />
                    ) : null}
                    {needsInspeccionApproval(tipoMaterialCode) ? (
                      <ApprovalLegBlock
                        title="Inspección"
                        approved={inspeccionApproved}
                        leg={inspeccionLeg}
                      />
                    ) : null}
                    {pendingMessage ? (
                      <Text style={{ color: brand.warningFg, fontWeight: 600 }}>{pendingMessage}</Text>
                    ) : null}
                  </div>
                )}

                {(canApprove || canReject) && (
                  <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
                    {canApprove ? (
                      <Button
                        appearance="primary"
                        size="small"
                        onClick={() => void changeStatus("approve")}
                        disabled={statusBusy}
                      >
                        {statusBusy ? "…" : "Aprobar"}
                      </Button>
                    ) : null}
                    {canReject ? (
                      <Button
                        appearance="secondary"
                        size="small"
                        onClick={() => void changeStatus("reject")}
                        disabled={statusBusy}
                      >
                        Rechazar
                      </Button>
                    ) : null}
                  </div>
                )}

                {!canChangeStatus && dynamicStatus === "CUARENTENA" ? (
                  <Text style={{ color: brand.muted, fontSize: 12 }}>
                    No tienes permiso para aprobar o rechazar este material.
                  </Text>
                ) : null}

                <Field
                  label="Estado Dynamics (solo referencia)"
                  value={asText(statusDynamicsRef)}
                />
                <Field label={LABELS.ubicacion} value={readDynamic(data, "ubicacion")} />
                <Field label={LABELS.almacen} value={readDynamic(data, "almacen")} />
                <Field label={LABELS.cantidad} value={dynamicCantidad} />
                <Field label={LABELS.fuente} value={fuenteDisplayLabel} />
              </div>
            </AppCard>
          </div>

          <AppCard className={s.historyFull}>
            <Text weight="semibold">{LABELS.scanHistory}</Text>
            <div style={{ marginTop: 12 }}>
              {scans === null ? null : scans.length === 0 ? (
                <EmptyState title={LABELS.noScans} hint={LABELS.noRecords} />
              ) : (
                <ScanHistoryTable events={scans} />
              )}
            </div>
            {canDownloadPdf && (
              <div style={{ marginTop: 12 }}>
                <Button
                  appearance="secondary"
                  size="small"
                  onClick={() =>
                    downloadAuditPdf(loteTrim, (msg) =>
                      toasts.push({ intent: "error", title: "Error", message: msg })
                    )
                  }
                >
                  {LABELS.downloadAuditPdf}
                </Button>
              </div>
            )}
          </AppCard>
        </>
      )}

      {status === "idle" && (
        <EmptyState title={LABELS.readyToLookup} hint="Ingresa un lote y presiona Buscar." />
      )}

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

function ApprovalLegBlock({
  title,
  approved,
  leg,
}: {
  title: string;
  approved: boolean;
  leg?: ApprovalLeg | null;
}) {
  if (!approved && !leg?.approved) {
    return (
      <div style={{ border: `1px solid ${brand.border}`, borderRadius: 10, padding: 10 }}>
        <Text weight="semibold">{title}</Text>
        <Text style={{ display: "block", marginTop: 4, color: brand.muted }}>Pendiente</Text>
      </div>
    );
  }
  const when = formatDateTime(leg?.at ?? null);
  const who = (leg?.actorEmail ?? "").trim() || "—";
  return (
    <div style={{ border: `1px solid ${brand.border}`, borderRadius: 10, padding: 10 }}>
      <Text weight="semibold">{title}: aprobada</Text>
      <Text style={{ display: "block", marginTop: 4 }}>Usuario: {who}</Text>
      <Text style={{ display: "block", marginTop: 2 }}>
        Fecha: {when.date} {when.time !== LABELS.noData ? when.time : ""}
      </Text>
    </div>
  );
}

function Field({ label, value, tooltip }: { label: string; value: React.ReactNode; tooltip?: string }) {
  const labelNode = tooltip ? (
    <Tooltip content={tooltip} relationship="label"><span>{label}</span></Tooltip>
  ) : label;
  return (
    <div style={{ border: `1px solid ${brand.border}`, borderRadius: 10, padding: 10 }}>
      <div style={{ color: brand.muted, fontSize: 12 }}>{labelNode}</div>
      <div style={{ marginTop: 4, fontWeight: 600 }}>{value}</div>
    </div>
  );
}

function CopyField({ label, value, onCopy }: { label: string; value: string; onCopy: (l: string, v: string) => void }) {
  const display = value ?? "—";
  return (
    <div style={{ border: `1px solid ${brand.border}`, borderRadius: 10, padding: 10 }}>
      <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: 8, color: brand.muted, fontSize: 12 }}>
        <span>{label}</span>
        <Button appearance="subtle" size="small" onClick={() => onCopy(label, display)}>Copiar</Button>
      </div>
      <div style={{ marginTop: 4, fontWeight: 600, wordBreak: "break-all" }}>{display}</div>
    </div>
  );
}
