import { useMemo, useRef, useState } from "react";
import { Button, Input, Text, makeStyles, shorthands } from "@fluentui/react-components";
import AppCard from "../components/ui/AppCard";
import { brand } from "../styles/brand";
import { api, ApiError } from "../api/client";

function logAudit(actionType: string, lote: string | null) {
  api("/audit/log", {
    method: "POST",
    body: { actionType, lote: lote || undefined },
    toast: false,
  }).catch(() => {});
}
import type { QrResponse } from "../api/types";
import { generateQrPlain } from "../utils/qrWithLogo";
import { exportLabelPreviewToPng } from "../utils/exportLabelPreview";
import LabelPreview from "../components/label/LabelPreview";

const useStyles = makeStyles({
  wrap: { display: "grid", gap: "24px", maxWidth: "600px" },
  title: { fontSize: "20px", fontWeight: 600, color: brand.text },
  row: { display: "grid", gap: "8px" },
  label: { fontSize: "14px", fontWeight: 500, color: brand.text2 },
  preview: {
    display: "grid",
    placeItems: "center",
    backgroundColor: brand.surface,
    borderRadius: "12px",
    ...shorthands.padding("16px"),
    boxShadow: "0 1px 3px rgba(0,0,0,0.06)",
    border: `1px solid ${brand.border}`,
  },
  actions: { display: "flex", gap: "10px", flexWrap: "wrap" },
  error: { color: brand.dangerFg, fontSize: "13px" },
});

export default function GenerateQrPage() {
  const s = useStyles();
  const previewRef = useRef<HTMLDivElement>(null);
  const [lote, setLote] = useState("");
  const [labelData, setLabelData] = useState<QrResponse["label"] | null>(null);
  const [dynamicData, setDynamicData] = useState<QrResponse["dynamic"] | null>(null);
  const [qrDataUrl, setQrDataUrl] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fileName = useMemo(() => {
    const safe = (lote || "QR").trim().replace(/[^\w\-]+/g, "_");
    return `Etiqueta_${safe}.png`;
  }, [lote]);

  const v = (lote || "").trim();

  async function generate() {
    if (!v) {
      setError("Escribe un lote.");
      return;
    }
    setBusy(true);
    setError(null);
    setQrDataUrl(null);
    setLabelData(null);
    setDynamicData(null);

    try {
      const qrResponse = await api<QrResponse>(`/qr/${encodeURIComponent(v)}`, { toast: false });
      const label = qrResponse?.label;
      if (!label) {
        setError("No se encontró etiqueta para este lote.");
        setBusy(false);
        return;
      }

      setLabelData(label);
      setDynamicData(qrResponse?.dynamic ?? null);

      const payload = label.publicToken
        ? `OLNQR:1:${label.publicToken}`
        : String(label.lote ?? v);

      const qrData = await generateQrPlain(payload, { width: 220, margin: 2 });
      setQrDataUrl(qrData);
      logAudit("GENERATE_LABEL", v);
    } catch (e) {
      const ae = e as ApiError;
      setError(
        ae?.status === 404
          ? "Lote no encontrado. Verifica el identificador."
          : ae?.status === 401 || ae?.status === 403
            ? "No tienes acceso. Inicia sesión."
            : (e as Error)?.message ?? "No se pudo generar la etiqueta."
      );
    } finally {
      setBusy(false);
    }
  }

  async function download() {
    const el = previewRef.current?.querySelector("[data-label-preview]") as HTMLElement | null;
    if (!el) return;
    try {
      setBusy(true);
      logAudit("DOWNLOAD_LABEL", v);
      const dataUrl = await exportLabelPreviewToPng(el);
      const a = document.createElement("a");
      a.href = dataUrl;
      a.download = fileName;
      document.body.appendChild(a);
      a.click();
      a.remove();
    } finally {
      setBusy(false);
    }
  }

  function scrollToPreview() {
    previewRef.current?.scrollIntoView({ behavior: "smooth", block: "center" });
  }

  return (
    <div className={s.wrap}>
      <h1 className={s.title}>Generar etiqueta imprimible</h1>

      <AppCard>
        <div style={{ display: "grid", gap: 16 }}>
          <div className={s.row}>
            <span className={s.label}>Lote</span>
            <Input
              value={lote}
              onChange={(_, d) => setLote(d.value)}
              placeholder="Ej: 251201-MEM0003454"
            />
          </div>

          <div className={s.actions}>
            <Button appearance="primary" onClick={generate} disabled={busy || !v}>
              {busy ? "Generando…" : "Generar etiqueta"}
            </Button>
            <Button appearance="secondary" onClick={scrollToPreview} disabled={!qrDataUrl}>
              Vista previa
            </Button>
            <Button appearance="secondary" onClick={download} disabled={!qrDataUrl || busy}>
              Descargar PNG
            </Button>
          </div>

          {error ? <div className={s.error}>{error}</div> : null}

          <div ref={previewRef} className={s.preview} style={{ overflowX: "auto" }}>
            {labelData && qrDataUrl ? (
              <div
                style={{
                  width: 400,
                  height: 300,
                  overflow: "hidden",
                }}
              >
                <div
                  style={{
                    width: 800,
                    height: 600,
                    transform: "scale(0.5)",
                    transformOrigin: "top left",
                  }}
                >
                  <LabelPreview
                    materialName={String(labelData.nombre ?? "").trim() || "—"}
                    codigo={String(labelData.codigo ?? "").trim() || "—"}
                    lote={String(labelData.lote ?? "").trim() || "—"}
                    fecha={labelData.fechaEntrada ?? "N/A"}
                    caducidad={
                      (labelData as any).fechaTipo === "REANALISIS"
                        ? ""
                        : ((labelData as any).fechaValor ?? labelData.caducidad ?? "")
                    }
                    reanalisis={
                      (labelData as any).fechaTipo === "REANALISIS"
                        ? ((labelData as any).fechaValor ?? labelData.reanalisis ?? "")
                        : ""
                    }
                    cantidad={(() => {
                      const manual = String((labelData as { cantidadPorEnvase?: string })?.cantidadPorEnvase ?? "").trim();
                      if (manual) return manual;
                      const d = dynamicData as { cantidad?: number | null; uom?: string | null } | null;
                      if (d != null && d.cantidad != null && typeof d.cantidad === "number")
                        return `${d.cantidad}${d.uom && String(d.uom).trim() ? " " + String(d.uom).trim() : ""}`;
                      return "N/A";
                    })()}
                    envaseNum={labelData.envaseNum ?? "—"}
                    envaseTotal={labelData.envaseTotal ?? "—"}
                    qrData={qrDataUrl}
                    logoUrl={`${import.meta.env.BASE_URL}logo-olnatura.png`}
                    documentCode={(labelData as any).documentCode ?? "AL-001-E02/04"}
                  />
                </div>
              </div>
            ) : (
              <Text style={{ opacity: 0.6 }}>Sin vista previa</Text>
            )}
          </div>
        </div>
      </AppCard>
    </div>
  );
}
