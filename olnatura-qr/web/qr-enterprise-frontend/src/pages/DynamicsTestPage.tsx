import { useCallback, useEffect, useState } from "react";
import { Button, Input, Text } from "@fluentui/react-components";
import AppCard from "../components/ui/AppCard";
import { brand } from "../styles/brand";
import { api, ApiError } from "../api/client";
import type { DynamicsConnectionStatus, DynamicsPreview } from "../api/types";
import { resolveDynamicsErrorView } from "../utils/dynamicsErrors";

function asText(v: string | number | null | undefined) {
  if (v === null || v === undefined) return "—";
  if (typeof v === "number") return String(v);
  const s = String(v).trim();
  return s.length > 0 ? s : "—";
}

function formatUtc(iso: string | null) {
  if (!iso) return "—";
  try {
    return new Date(iso).toLocaleString();
  } catch {
    return iso;
  }
}

const ROWS: { key: keyof DynamicsPreview; label: string }[] = [
  { key: "itemNumber", label: "Código" },
  { key: "productName", label: "Nombre" },
  { key: "productType", label: "Tipo producto" },
  { key: "productGroup", label: "Grupo producto" },
  { key: "site", label: "Sitio" },
  { key: "warehouse", label: "Almacén" },
  { key: "availableQuantity", label: "Existencia disponible" },
  { key: "onHandQuantity", label: "Existencia total" },
  { key: "unit", label: "Unidad" },
  { key: "batchNumber", label: "Lote" },
  { key: "batchAttribute", label: "Atributo lote" },
  { key: "batchValue", label: "Valor atributo" },
  { key: "qualityResult", label: "Resultado calidad" },
  { key: "qualityValue", label: "Valor calidad" },
];

export default function DynamicsTestPage() {
  const [codigo, setCodigo] = useState("");
  const [lote, setLote] = useState("");
  const [busy, setBusy] = useState(false);
  const [statusBusy, setStatusBusy] = useState(false);
  const [err, setErr] = useState<{ title: string; hint: string; code?: string; elapsedMs?: number } | null>(null);
  const [data, setData] = useState<DynamicsPreview | null>(null);
  const [status, setStatus] = useState<DynamicsConnectionStatus | null>(null);

  const loadStatus = useCallback(async () => {
    try {
      setStatusBusy(true);
      const s = await api<DynamicsConnectionStatus>("/dynamics/status", { toast: false });
      setStatus(s);
    } catch {
      setStatus(null);
    } finally {
      setStatusBusy(false);
    }
  }, []);

  useEffect(() => {
    void loadStatus();
    const id = window.setInterval(() => void loadStatus(), 30_000);
    return () => window.clearInterval(id);
  }, [loadStatus]);

  const onConsultar = async () => {
    setErr(null);
    setData(null);
    const item = codigo.trim();
    const batch = lote.trim();
    if (!item && !batch) {
      setErr({ title: "Datos incompletos", hint: "Indica código o lote." });
      return;
    }
    const params = new URLSearchParams();
    if (item) params.set("itemNumber", item);
    if (batch) params.set("lote", batch);
    const qs = params.toString();
    try {
      setBusy(true);
      const res = await api<DynamicsPreview>(`/dynamics/preview${qs ? `?${qs}` : ""}`, { toast: false });
      setData(res);
      void loadStatus();
    } catch (e) {
      const ae = e as ApiError;
      const view = resolveDynamicsErrorView(ae?.body, ae?.message ?? "No se pudo consultar Dynamics.");
      setErr(view);
    } finally {
      setBusy(false);
    }
  };

  return (
    <div style={{ display: "grid", gap: 24, maxWidth: 900 }}>
      <div>
        <h1 style={{ fontSize: 20, fontWeight: 600, color: brand.text, margin: 0 }}>
          Prueba Dynamics (OData)
        </h1>
        <Text style={{ fontSize: 13, color: brand.muted, marginTop: 6, display: "block" }}>
          Laboratorio de conexión: tiempos, caducidad de token (refresh cada 65 min) y errores tipificados.
        </Text>
      </div>

      <AppCard style={{ display: "grid", gap: 10 }}>
        <Text style={{ fontSize: 15, fontWeight: 600, color: brand.text }}>Estado de conexión Olnatura → Dynamics</Text>
        {statusBusy && !status ? (
          <Text style={{ color: brand.muted }}>Cargando estado…</Text>
        ) : status ? (
          <div style={{ display: "grid", gap: 6, fontSize: 13, color: brand.text }}>
            <div>Modo: <strong>{status.mode}</strong> · Refresh: <strong>{status.tokenRefreshInterval}</strong></div>
            <div>Token válido: <strong>{status.tokenValid ? "Sí" : "No"}</strong>
              {status.tokenExpired ? " (expirado en Azure)" : ""}
              {" · "}Expira: <strong>{formatUtc(status.tokenExpiresAtUtc)}</strong>
              {" · "}Quedan: <strong>{status.secondsUntilTokenExpiry}s</strong>
            </div>
            <div>Última renovación OAuth: <strong>{formatUtc(status.lastTokenRefreshAtUtc)}</strong>
              {status.lastTokenRefreshMs > 0 ? ` (${status.lastTokenRefreshMs} ms)` : ""}
            </div>
            {status.lastError ? (
              <Text style={{ color: brand.dangerFg }}>Último error OAuth: {status.lastError}</Text>
            ) : null}
          </div>
        ) : (
          <Text style={{ color: brand.muted }}>No se pudo leer el estado (¿backend apagado?).</Text>
        )}
        <Button appearance="secondary" size="small" onClick={() => void loadStatus()} disabled={statusBusy}>
          Actualizar estado
        </Button>
      </AppCard>

      <AppCard style={{ display: "grid", gap: 14 }}>
        <Field label="Código (itemNumber)" value={codigo} onChange={setCodigo} placeholder="Ej. 400606160200" />
        <Field label="Lote" value={lote} onChange={setLote} placeholder="Ej. 260518-MEM0003583" />
        <Button appearance="primary" onClick={onConsultar} disabled={busy}>
          {busy ? "Consultando…" : "Consultar Dynamics"}
        </Button>
        {err ? (
          <div style={{ padding: 12, borderRadius: 8, background: brand.dangerBg, border: `1px solid ${brand.border}` }}>
            <Text weight="semibold" style={{ color: brand.dangerFg, display: "block" }}>{err.title}</Text>
            <Text style={{ color: brand.dangerFg, fontSize: 13, marginTop: 4, display: "block" }}>{err.hint}</Text>
            {err.code ? (
              <Text style={{ color: brand.muted, fontSize: 12, marginTop: 6, display: "block" }}>
                Código: {err.code}{err.elapsedMs != null ? ` · ${err.elapsedMs} ms` : ""}
              </Text>
            ) : null}
          </div>
        ) : null}
      </AppCard>

      {data ? (
        <AppCard style={{ display: "grid", gap: 0 }}>
          <Text style={{ fontSize: 15, fontWeight: 600, color: brand.text, marginBottom: 4 }}>
            Resultado consolidado
          </Text>
          <Text style={{ fontSize: 12, color: brand.muted, marginBottom: 12 }}>
            Tiempo total de consulta: <strong>{data.elapsedMs} ms</strong>
          </Text>
          <table style={{ width: "100%", borderCollapse: "collapse", fontSize: 14 }}>
            <tbody>
              {ROWS.map((row) => (
                <tr key={row.key} style={{ borderTop: `1px solid ${brand.border}` }}>
                  <td style={{ padding: "10px 12px", fontWeight: 600, width: "40%", color: brand.text }}>
                    {row.label}
                  </td>
                  <td style={{ padding: "10px 12px", color: brand.text }}>
                    {asText(data[row.key] as string | number | null | undefined)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </AppCard>
      ) : null}
    </div>
  );
}

function Field({
  label,
  value,
  onChange,
  placeholder,
}: {
  label: string;
  value: string;
  onChange: (v: string) => void;
  placeholder?: string;
}) {
  return (
    <div style={{ display: "grid", gap: 6 }}>
      <Text weight="semibold">{label}</Text>
      <Input value={value} placeholder={placeholder} onChange={(_, d) => onChange(d.value)} />
    </div>
  );
}
