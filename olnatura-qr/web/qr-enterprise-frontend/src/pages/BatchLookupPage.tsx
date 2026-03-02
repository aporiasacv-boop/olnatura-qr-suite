import React, { useMemo, useState } from "react";
import { Button, Card, Input, Text, Select, Option } from "@fluentui/react-components";
import { api, ApiError } from "../api/client";
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

// Page component
const STATUS_OPTIONS = ["LIBERADO", "APROBADO", "CUARENTENA", "RECHAZADO", "PENDIENTE"] as const;

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

  const loteTrim = useMemo(() => lote.trim(), [lote]);
  const canChangeStatus = hasRole("INSPECCION") || hasRole("ADMIN");

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
      toasts.push({ intent: "success", title: "Estatus actualizado", message: `Nuevo estatus: ${newStatus}` });
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
            value={lote}
            onChange={(_, d) => setLote(d.value)}
            placeholder="Ej. LOT-2026-001-A"
          />
        </div>

        <Button
          appearance="primary"
          onClick={() => void load()}
          disabled={!loteTrim || status === "loading"}
        >
          Buscar
        </Button>

        {can("SCAN") && (
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
              <Field label="Código" value={readLabel(data, "codigo")} />
              <Field label="Lote" value={readLabel(data, "lote")} />
              <Field label="Fecha entrada" value={readLabel(data, "fechaEntrada")} />
              <Field label="Caducidad" value={readLabel(data, "caducidad")} />
              <Field label="Reanálisis" value={readLabel(data, "reanalisis")} />
              <Field label="Envase" value={labelEnvase} />
            </div>

            <div style={{ marginTop: 14, color: "#6B6B6B", fontSize: 12 }}>
              Descargar etiqueta (PNG): disponible desde la pantalla de registrar etiqueta o generar etiqueta.
            </div>
          </Card>

          <div style={{ display: "grid", gap: 14 }}>
            <Card style={{ padding: 16 }}>
              <Text weight="semibold">Estado dinámico</Text>

              <div style={{ marginTop: 12, display: "grid", gap: 10 }}>
                <div style={{ display: "flex", alignItems: "center", gap: 10, flexWrap: "wrap" }}>
                  <Text>Estado</Text>
                  <StatusTag status={dynamicStatus} />
                  {canChangeStatus && (
                    <>
                      <Select
                        value={newStatus}
                        onChange={(_, d) => setNewStatus(d.optionValue ?? "")}
                        placeholder="Cambiar a…"
                        style={{ minWidth: 140 }}
                      >
                        {STATUS_OPTIONS.map((s) => (
                          <Option key={s} value={s}>{s}</Option>
                        ))}
                      </Select>
                      <Button
                        appearance="primary"
                        size="small"
                        onClick={() => void changeStatus()}
                        disabled={!newStatus || statusBusy}
                      >
                        {statusBusy ? "…" : "Aplicar"}
                      </Button>
                    </>
                  )}
                </div>

                <Field label="Ubicación" value={readDynamic(data, "ubicacion")} />
                <Field label="Cantidad" value={dynamicCantidad} />
                <Field label="Fuente" value={readDynamic(data, "fuente")} />
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
    </div>
  );
}

// Sub-components
function Field({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div style={{ border: "1px solid #E6E6E6", borderRadius: 10, padding: 10 }}>
      <div style={{ color: "#6B6B6B", fontSize: 12 }}>{label}</div>
      <div style={{ marginTop: 4, fontWeight: 600 }}>{value}</div>
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