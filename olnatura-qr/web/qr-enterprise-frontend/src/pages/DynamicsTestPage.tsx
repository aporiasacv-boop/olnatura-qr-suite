import { useState } from "react";
import { Button, Input, Text } from "@fluentui/react-components";
import AppCard from "../components/ui/AppCard";
import { brand } from "../styles/brand";
import { api, ApiError } from "../api/client";
import type { DynamicsPreview } from "../api/types";

function asText(v: string | number | null | undefined) {
  if (v === null || v === undefined) return "—";
  if (typeof v === "number") return String(v);
  const s = String(v).trim();
  return s.length > 0 ? s : "—";
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
  const [err, setErr] = useState<string | null>(null);
  const [data, setData] = useState<DynamicsPreview | null>(null);

  const onConsultar = async () => {
    setErr(null);
    setData(null);
    const item = codigo.trim();
    const batch = lote.trim();
    if (!item && !batch) {
      setErr("Indica código o lote.");
      return;
    }
    const params = new URLSearchParams();
    if (item) params.set("itemNumber", item);
    if (batch) params.set("lote", batch);
    const qs = params.toString();
    try {
      setBusy(true);
      const res = await api<DynamicsPreview>(`/dynamics/preview${qs ? `?${qs}` : ""}`);
      setData(res);
    } catch (e) {
      const ae = e as ApiError;
      setErr(ae?.message ?? "No se pudo consultar Dynamics.");
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
          Pantalla temporal para validar integración Backend OAuth → Dynamics F&amp;O.
        </Text>
      </div>

      <AppCard style={{ display: "grid", gap: 14 }}>
        <Field label="Código (itemNumber)" value={codigo} onChange={setCodigo} placeholder="Ej. 000123" />
        <Field label="Lote" value={lote} onChange={setLote} placeholder="Ej. 251201-MEM0003454" />
        <Button appearance="primary" onClick={onConsultar} disabled={busy}>
          {busy ? "Consultando…" : "Consultar Dynamics"}
        </Button>
        {err ? <Text style={{ color: brand.dangerFg }}>{err}</Text> : null}
      </AppCard>

      {data ? (
        <AppCard style={{ display: "grid", gap: 0 }}>
          <Text style={{ fontSize: 15, fontWeight: 600, color: brand.text, marginBottom: 12 }}>
            Resultado consolidado
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
