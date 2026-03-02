import { useMemo, useState } from "react";
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
import { generateQrWithLogo } from "../utils/qrWithLogo";
import { renderLabelToPng, type FechaTipo } from "../utils/labelToPng";

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
  const [lote, setLote] = useState("");
  const [labelData, setLabelData] = useState<QrResponse["label"] | null>(null);
  const [pngDataUrl, setPngDataUrl] = useState<string | null>(null);
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
    setPngDataUrl(null);
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

      const payload = String(label.lote ?? v);
      const logoPath = `${import.meta.env.BASE_URL}logo-olnatura.png`;

      const qrWithLogo = await generateQrWithLogo(payload, {
        errorCorrectionLevel: "H",
        margin: 4,
        width: 800,
        logoUrl: logoPath,
        logoSizeRatio: 0.22,
      });

      const fechaTipo: FechaTipo = (label as any).fechaTipo ?? (label.reanalisis?.trim() && !label.caducidad?.trim() ? "REANALISIS" : "CADUCIDAD");
      const fechaValor = (label as any).fechaValor ?? (fechaTipo === "CADUCIDAD" ? label.caducidad : label.reanalisis);
      const fullLabelPng = await renderLabelToPng(
        {
          tipoMaterial: label.tipoMaterial,
          nombre: label.nombre,
          codigo: label.codigo,
          lote: label.lote,
          fechaEntrada: label.fechaEntrada,
          fechaTipo,
          fechaValor,
          caducidad: label.caducidad,
          reanalisis: label.reanalisis,
          envaseNum: label.envaseNum,
          envaseTotal: label.envaseTotal,
        },
        qrWithLogo
      );

      setPngDataUrl(fullLabelPng);
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

  function download() {
    if (!pngDataUrl) return;
    logAudit("DOWNLOAD_LABEL", v);
    const a = document.createElement("a");
    a.href = pngDataUrl;
    a.download = fileName;
    document.body.appendChild(a);
    a.click();
    a.remove();
  }

  function printLabel() {
    if (!pngDataUrl) return;
    const w = window.open("", "_blank");
    if (!w) return;
    w.document.write(`
      <!DOCTYPE html><html><head><title>Etiqueta</title></head>
      <body style="margin:0;display:flex;justify-content:center;align-items:center;min-height:100vh;">
        <img src="${pngDataUrl}" alt="Etiqueta" style="max-width:100%;height:auto;" />
      </body></html>
    `);
    w.document.close();
    w.focus();
    w.print();
    w.close();
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
            <Button onClick={download} disabled={!pngDataUrl}>
              Descargar PNG
            </Button>
            <Button appearance="secondary" onClick={printLabel} disabled={!pngDataUrl}>
              Imprimir
            </Button>
          </div>

          {error ? <Text style={{ color: "#B10E1C" }}>{error}</Text> : null}

          <div className={s.preview}>
            {pngDataUrl ? (
              <img src={pngDataUrl} alt="Etiqueta preview" className={s.img} />
            ) : (
              <Text style={{ opacity: 0.6 }}>Vista previa aquí</Text>
            )}
          </div>
        </div>
      </Card>
    </div>
  );
}
