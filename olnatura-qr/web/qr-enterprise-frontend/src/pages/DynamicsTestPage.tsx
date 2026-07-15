import { useState } from "react";
import { Button, Input, Text } from "@fluentui/react-components";
import AppCard from "../components/ui/AppCard";
import { brand } from "../styles/brand";
import { api, ApiError } from "../api/client";
import type { DynamicsLookupResponse } from "../api/types";
import { resolveDynamicsErrorView } from "../utils/dynamicsErrors";

function asText(v: string | number | null | undefined) {
  if (v === null || v === undefined) return "—";
  if (typeof v === "number") return String(v);
  const s = String(v).trim();
  return s.length > 0 ? s : "—";
}

const ROWS: { key: keyof DynamicsLookupResponse; label: string }[] = [
  { key: "codigo", label: "Código" },
  { key: "nombre", label: "Nombre" },
  { key: "lote", label: "Lote" },
  { key: "caducidad", label: "Caducidad" },
  { key: "almacen", label: "Almacén / tipo material" },
  { key: "ubicacion", label: "Ubicación" },
  { key: "statusDynamics", label: "Estado Dynamics" },
  { key: "cantidadAlmacen", label: "Inventario disponible (solo consulta)" },
  { key: "fuente", label: "Fuente" },
];

/**
 * Pantalla de prueba (heredada 28 may) cableada al lookup de julio:
 * token OAuth nuevo por búsqueda via GET /api/v1/dynamics/lookup/{lote}.
 */
export default function DynamicsTestPage() {
  const [lote, setLote] = useState("");
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState<{ title: string; hint: string; code?: string } | null>(null);
  const [data, setData] = useState<DynamicsLookupResponse | null>(null);

  const onConsultar = async () => {
    setErr(null);
    setData(null);
    const batch = lote.trim();
    if (!batch) {
      setErr({ title: "Datos incompletos", hint: "Indica el lote (BatchNumber)." });
      return;
    }
    try {
      setBusy(true);
      const res = await api<DynamicsLookupResponse>(
        `/dynamics/lookup/${encodeURIComponent(batch)}`,
        { toast: false }
      );
      setData(res);
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
        <Text style={{ color: brand.muted, fontSize: 13, marginTop: 6, display: "block" }}>
          Consulta por lote usando el mismo flujo de registro (token nuevo en cada búsqueda).
        </Text>
      </div>

      <AppCard style={{ display: "grid", gap: 12 }}>
        <div style={{ display: "grid", gap: 6 }}>
          <Text style={{ fontSize: 14, fontWeight: 500 }}>Lote</Text>
          <Input
            value={lote}
            onChange={(_, d) => setLote(d.value ?? "")}
            placeholder="Ej. 260713-MEM0003662"
          />
        </div>
        <Button appearance="primary" onClick={() => void onConsultar()} disabled={busy || !lote.trim()}>
          {busy ? "Consultando…" : "Consultar Dynamics"}
        </Button>
        {err ? (
          <div style={{ color: brand.dangerFg, fontSize: 13 }}>
            <div style={{ fontWeight: 600 }}>{err.title}</div>
            <div>{err.hint}</div>
            {err.code ? <div style={{ color: brand.muted }}>Código: {err.code}</div> : null}
          </div>
        ) : null}
      </AppCard>

      {data ? (
        <AppCard style={{ display: "grid", gap: 8 }}>
          <Text weight="semibold">Resultado lookup</Text>
          {ROWS.map((r) => (
            <div key={r.key} style={{ display: "flex", gap: 12, fontSize: 14 }}>
              <Text style={{ minWidth: 220, color: brand.muted }}>{r.label}</Text>
              <Text>{asText(data[r.key] as string | number | null)}</Text>
            </div>
          ))}
        </AppCard>
      ) : null}
    </div>
  );
}
