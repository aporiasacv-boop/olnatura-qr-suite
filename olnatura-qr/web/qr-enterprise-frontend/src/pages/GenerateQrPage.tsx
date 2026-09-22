import { useRef, useState } from "react";
import { Button, Input, Link, Text, makeStyles, shorthands } from "@fluentui/react-components";
import AppCard from "../components/ui/AppCard";
import { brand } from "../styles/brand";
import { api, ApiError } from "../api/client";
import type { QrResponse } from "../api/types";
import { generateQrPlain } from "../utils/qrWithLogo";
import { downloadLabelZplFile } from "../utils/downloadLabelZpl";
import LabelPreview from "../components/label/LabelPreview";
import LoteAutocomplete from "../components/ui/LoteAutocomplete";
import ZplPrintHelpDialog from "../components/ui/ZplPrintHelpDialog";
import { formatNumber } from "../utils/formatNumber";
import {
  parseEnvaseTotal,
  validateReprintRange,
} from "../utils/labelPreviewPermissions";
import { cantidadForEnvase, cantidadTotalOf, isRestosEnabled } from "../utils/envaseRestos";
import { storedDateText } from "../utils/dateFormat";
import { resolveLabelDocumentCode } from "../utils/labelDocumentCode";

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
  subtitle: { fontSize: "13px", color: brand.muted, marginTop: "-12px" },
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
  printBox: {
    marginTop: "16px",
    display: "grid",
    gap: "10px",
    ...shorthands.padding("12px"),
    ...shorthands.border("1px", "solid", brand.border),
    ...shorthands.borderRadius("10px"),
    backgroundColor: brand.surface,
  },
});

export default function GenerateQrPage() {
  const s = useStyles();
  const previewRef = useRef<HTMLDivElement>(null);
  const [lote, setLote] = useState("");
  const [labelData, setLabelData] = useState<QrResponse["label"] | null>(null);
  const [qrDataUrl, setQrDataUrl] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [zplBusy, setZplBusy] = useState(false);
  const [zplHelpOpen, setZplHelpOpen] = useState(false);
  const [printFrom, setPrintFrom] = useState("1");
  const [printTo, setPrintTo] = useState("1");
  const [error, setError] = useState<string | null>(null);

  const v = (lote || "").trim();
  const hasPreview = !!(labelData && qrDataUrl);
  const envaseTotal = parseEnvaseTotal(labelData);

  async function loadLabel() {
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

      const total = parseEnvaseTotal(label);
      setPrintFrom("1");
      setPrintTo(String(total));

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
                : ae?.message ?? (e as Error)?.message ?? "No se pudo cargar la etiqueta."
      );
    } finally {
      setBusy(false);
    }
  }

  async function reprintZpl() {
    if (!labelData) {
      setError("Primero busca el lote para reimprimir.");
      return;
    }

    const validated = validateReprintRange(printFrom, printTo, envaseTotal, formatNumber);
    if (!validated.ok) {
      setError(validated.message);
      return;
    }

    setZplBusy(true);
    setError(null);
    try {
      const key = String(labelData.id ?? labelData.lote ?? v).trim();
      await downloadLabelZplFile({
        labelIdOrLote: key,
        totalEnvases: envaseTotal,
        printFrom: validated.from,
        printTo: validated.to,
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
      <h1 className={s.title}>Generar etiqueta</h1>
      <Text className={s.subtitle}>
        Reimpresión de etiquetas ya registradas. Solo rangos dentro del total original del lote.
      </Text>

      <AppCard>
        <form
          style={{ display: "grid", gap: 16 }}
          onSubmit={(e) => {
            e.preventDefault();
            void loadLabel();
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
              {busy ? "Buscando…" : "Buscar lote"}
            </Button>
            <Button
              appearance="secondary"
              type="button"
              onClick={scrollToPreview}
              disabled={!hasPreview || busy || zplBusy}
            >
              Vista previa
            </Button>
          </div>

          {error ? <div className={s.error}>{error}</div> : null}
        </form>

        {hasPreview ? (
          <div className={s.printBox}>
            <Text weight="semibold">Rango a reimprimir</Text>
            <Text style={{ fontSize: 12, color: brand.muted }}>
              Este lote tiene <strong>{formatNumber(envaseTotal)}</strong> envase(s) registrado(s). Solo puedes
              reimprimir del 1 al {formatNumber(envaseTotal)}.
            </Text>
            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 8 }}>
              <div>
                <Text style={{ fontSize: 12, color: brand.muted }}>Desde</Text>
                <Input
                  type="number"
                  min={1}
                  max={envaseTotal}
                  value={printFrom}
                  onChange={(_, d) => {
                    setPrintFrom(d.value ?? "");
                    setError(null);
                  }}
                  placeholder="1"
                  disabled={zplBusy}
                />
              </div>
              <div>
                <Text style={{ fontSize: 12, color: brand.muted }}>Hasta</Text>
                <Input
                  type="number"
                  min={1}
                  max={envaseTotal}
                  value={printTo}
                  onChange={(_, d) => {
                    setPrintTo(d.value ?? "");
                    setError(null);
                  }}
                  placeholder={String(envaseTotal)}
                  disabled={zplBusy}
                />
              </div>
            </div>
            <div className={s.actions}>
              <Button
                appearance="primary"
                type="button"
                onClick={() => void reprintZpl()}
                disabled={zplBusy || busy}
              >
                {zplBusy ? "Descargando…" : "Reimprimir (Zebra .zpl)"}
              </Button>
              <Link
                onClick={() => setZplHelpOpen(true)}
                style={{ alignSelf: "center", fontSize: 13 }}
              >
                Cómo imprimir
              </Link>
            </div>
          </div>
        ) : null}

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
                  fecha={storedDateText(labelData.fechaEntrada) || "N/A"}
                  caducidad={storedDateText(labelData.caducidad)}
                  reanalisis={storedDateText(labelData.reanalisis)}
                  cantidad={(() => {
                    const n = Number(printTo);
                    const envase = Number.isFinite(n) && n >= 1 ? n : parseEnvaseTotal(labelData);
                    return cantidadForEnvase(labelData, envase);
                  })()}
                  envaseNum={(() => {
                    const n = Number(printTo);
                    if (Number.isFinite(n) && n >= 1) return n;
                    return isRestosEnabled(labelData)
                      ? labelData.envaseTotal ?? "—"
                      : labelData.envaseNum ?? "—";
                  })()}
                  envaseTotal={labelData.envaseTotal ?? "—"}
                  cantidadTotal={cantidadTotalOf(labelData)}
                  qrData={qrDataUrl!}
                  logoUrl={`${import.meta.env.BASE_URL}logo-olnatura.png`}
                  documentCode={resolveLabelDocumentCode((labelData as any).documentCode)}
                  tipoMaterial={String(labelData.tipoMaterial ?? "").trim() || null}
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
