import { useMemo, useRef, useState } from "react";
import { Button, Card, Input, Text, makeStyles, shorthands } from "@fluentui/react-components";
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
  wrap: { display: "grid", gap: "14px", maxWidth: "600px" },
  row: { display: "grid", gap: "6px" },
  preview: {
    display: "grid",
    placeItems: "center",
    backgroundColor: "#fff",
    borderRadius: "12px",
    ...shorthands.padding("16px"),
    boxShadow: "0 1px 2px rgba(0,0,0,0.08)",
    border: "1px solid #E5E7EB",
  },
  img: { maxWidth: "100%", height: "auto", objectFit: "contain" },
  actions: { display: "flex", gap: "10px", flexWrap: "wrap" },
  labelText: { fontSize: "12px", color: "#6B7280" },
  labelValue: { fontSize: "14px", fontWeight: 600 },
});

export default function GenerateQrPage() {
  const s = useStyles();
  const previewRef = useRef<HTMLDivElement>(null);
  const [lote, setLote] = useState("");
  const [labelData, setLabelData] = useState<QrResponse["label"] | null>(null);
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

    try {
      const qrResponse = await api<QrResponse>(`/qr/${encodeURIComponent(v)}`, { toast: false });
      const label = qrResponse?.label;
      if (!label) {
        setError("No se encontró etiqueta para este lote.");
        setBusy(false);
        return;
      }

      setLabelData(label);

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
      <Text size={600} weight="semibold">
        Generar etiqueta imprimible
      </Text>
      <Text size={300} style={{ opacity: 0.75 }}>
        Solo ADMIN/ALMACÉN. Busca por lote y genera la etiqueta con datos estáticos y QR con logo. El estatus no se imprime.
      </Text>

      <Card>
        <div style={{ padding: 16, display: "grid", gap: 12 }}>
          <div className={s.row}>
            <Text>Lote</Text>
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

          {error ? <Text style={{ color: "#B10E1C" }}>{error}</Text> : null}

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
                    cantidad="N/A"
                    envaseNum={labelData.envaseNum ?? "—"}
                    envaseTotal={labelData.envaseTotal ?? "—"}
                    qrData={qrDataUrl}
                    logoUrl={`${import.meta.env.BASE_URL}logo-olnatura.png`}
                    documentCode={(labelData as any).documentCode ?? "AL-001-E02/04"}
                  />
                </div>
              </div>
            ) : (
              <Text style={{ opacity: 0.6 }}>Vista previa aquí</Text>
            )}
          </div>
        </div>
      </Card>
    </div>
  );
}
