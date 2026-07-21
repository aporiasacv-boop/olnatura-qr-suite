import { useRef, useState } from "react";
import { Button, Text, Link, makeStyles, shorthands } from "@fluentui/react-components";
import AppCard from "../components/ui/AppCard";
import { brand } from "../styles/brand";
import { api, ApiError } from "../api/client";
import type { QrResponse } from "../api/types";
import { generateQrPlain } from "../utils/qrWithLogo";
import { downloadLabelZplFile } from "../utils/downloadLabelZpl";
import LabelPreview from "../components/label/LabelPreview";
import LoteAutocomplete from "../components/ui/LoteAutocomplete";
import ZplPrintHelpDialog from "../components/ui/ZplPrintHelpDialog";

function logAudit(actionType: string, lote: string | null) {
  api("/audit/log", {
    method: "POST",
    body: { actionType, lote: lote || undefined },
    toast: false,
  }).catch(() => {});
}

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

function parseEnvaseTotal(label: Record<string, unknown> | null): number {
  if (!label) return 1;
  const raw = label.envaseTotal ?? label.totalEnvases ?? 1;
  const n = typeof raw === "number" ? raw : parseInt(String(raw), 10);
  return Number.isFinite(n) && n >= 1 ? n : 1;
}

export default function GenerateQrPage() {
  const s = useStyles();
  const previewRef = useRef<HTMLDivElement>(null);
  const [lote, setLote] = useState("");
  const [labelData, setLabelData] = useState<QrResponse["label"] | null>(null);
  const [qrDataUrl, setQrDataUrl] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [zplBusy, setZplBusy] = useState(false);
  const [zplHelpOpen, setZplHelpOpen] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const v = (lote || "").trim();
  const hasPreview = !!(labelData && qrDataUrl);

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
      const isDynamics =
        ae?.status === 502 ||
        ae?.status === 504 ||
        (typeof ae?.body?.error === "string" && String(ae.body.error).startsWith("DYNAMICS_"));
      setError(
        ae?.status === 404
          ? "Lote no encontrado. Verifica el identificador."
          : ae?.status === 409
            ? ae.message || "Este lote no está activo para operación."
            : ae?.status === 401 || ae?.status === 403
              ? "No tienes acceso. Inicia sesión."
              : isDynamics
                ? ae.message || "Dynamics 365 no disponible. Intenta de nuevo."
                : ae?.message ?? (e as Error)?.message ?? "No se pudo generar la etiqueta."
      );
    } finally {
      setBusy(false);
    }
  }

  async function downloadZpl() {
    if (!labelData) {
      setError("Primero genera la etiqueta.");
      return;
    }
    setZplBusy(true);
    setError(null);
    try {
      const key = String(labelData.id ?? labelData.lote ?? v).trim();
      const total = parseEnvaseTotal(labelData);
      await downloadLabelZplFile({
        labelIdOrLote: key,
        totalEnvases: total,
        printFrom: 1,
        printTo: total,
      });
    } catch (e) {
      setError(
        (e as Error)?.message?.trim() ||
          "No se pudo descargar la etiqueta Zebra (.zpl). Comprueba la sesión e intenta de nuevo."
      );
    } finally {
      setZplBusy(false);
    }
  }

  function scrollToPreview() {
    previewRef.current?.scrollIntoView({ behavior: "smooth", block: "center" });
  }

  return (
    <div className={s.wrap}>
      <h1 className={s.title}>Generar etiqueta imprimible</h1>

      <AppCard>
        <form
          style={{ display: "grid", gap: 16 }}
          onSubmit={(e) => {
            e.preventDefault();
            void generate();
          }}
        >
          <div className={s.row}>
            <span className={s.label}>Lote</span>
            <LoteAutocomplete
              value={lote}
              onChange={setLote}
              onSelect={(item) => setLote(item.lote)}
              placeholder="Ej: 251201-MEM0003454"
            />
          </div>

          <div className={s.actions}>
            <Button appearance="primary" type="submit" disabled={busy || zplBusy || !v}>
              {busy ? "Generando…" : "Generar etiqueta"}
            </Button>
            <Button
              appearance="secondary"
              type="button"
              onClick={scrollToPreview}
              disabled={!hasPreview || busy || zplBusy}
            >
              Vista previa
            </Button>
            <Button
              appearance="primary"
              type="button"
              onClick={() => void downloadZpl()}
              disabled={!hasPreview || busy || zplBusy}
            >
              {zplBusy ? "Descargando…" : "Descargar Zebra (.zpl)"}
            </Button>
            <Link
              onClick={() => setZplHelpOpen(true)}
              style={{ alignSelf: "center", fontSize: 13 }}
            >
              Cómo imprimir
            </Link>
          </div>

          {error ? <div className={s.error}>{error}</div> : null}
        </form>

        <div ref={previewRef} className={s.preview} style={{ overflowX: "auto", marginTop: 16 }}>
          {hasPreview ? (
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
                    const manual = String(
                      (labelData as { cantidadPorEnvase?: string })?.cantidadPorEnvase ?? ""
                    ).trim();
                    return manual || "N/A";
                  })()}
                  envaseNum={labelData.envaseNum ?? "—"}
                  envaseTotal={labelData.envaseTotal ?? "—"}
                  qrData={qrDataUrl!}
                  logoUrl={`${import.meta.env.BASE_URL}logo-olnatura.png`}
                  documentCode={(labelData as any).documentCode ?? "AL-001-E02/04"}
                />
              </div>
            </div>
          ) : (
            <Text style={{ opacity: 0.6 }}>Sin vista previa</Text>
          )}
        </div>
      </AppCard>

      <ZplPrintHelpDialog open={zplHelpOpen} onOpenChange={setZplHelpOpen} />
    </div>
  );
}
