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

function parseEnvaseTotal(label: Record<string, unknown> | null): number {
  if (!label) return 1;
  const raw = label.envaseTotal ?? label.totalEnvases ?? 1;
  const n = typeof raw === "number" ? raw : parseInt(String(raw), 10);
  return Number.isFinite(n) && n >= 1 ? n : 1;
}

/** Valida reimpresión: solo números enteros dentro de 1..envaseTotal registrado. */
function validateReprintRange(
  fromRaw: string,
  toRaw: string,
  envaseTotal: number
): { ok: true; from: number; to: number } | { ok: false; message: string } {
  const from = Number.parseInt(String(fromRaw).trim(), 10);
  const to = Number.parseInt(String(toRaw).trim(), 10);

  if (!Number.isFinite(from) || !Number.isFinite(to) || String(fromRaw).trim() === "" || String(toRaw).trim() === "") {
    return {
      ok: false,
      message: `Indica un rango válido. Este lote tiene ${envaseTotal} envase(s) registrado(s) (permitido: 1 a ${envaseTotal}).`,
    };
  }
  if (!Number.isInteger(from) || !Number.isInteger(to)) {
    return {
      ok: false,
      message: "Desde y Hasta deben ser números enteros.",
    };
  }
  if (from < 1 || to < 1) {
    return {
      ok: false,
      message: "El rango debe comenzar en 1. No se permiten números menores a 1.",
    };
  }
  if (from > envaseTotal || to > envaseTotal) {
    return {
      ok: false,
      message: `Rango inválido: solo existen etiquetas del 1 al ${envaseTotal} para este lote. No se puede reimprimir el envase ${Math.max(from, to)}.`,
    };
  }
  if (from > to) {
    return {
      ok: false,
      message: "Desde no puede ser mayor que Hasta.",
    };
  }
  return { ok: true, from, to };
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

    const validated = validateReprintRange(printFrom, printTo, envaseTotal);
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
              Este lote tiene <strong>{envaseTotal}</strong> envase(s) registrado(s). Solo puedes
              reimprimir del 1 al {envaseTotal}.
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
