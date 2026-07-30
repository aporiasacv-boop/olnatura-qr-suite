import { useEffect, useMemo, useRef, useState } from "react";
import {
  Button,
  Input,
  Link,
  Text,
  makeStyles,
  shorthands,
} from "@fluentui/react-components";
import AppCard from "../ui/AppCard";
import EmptyState from "../ui/EmptyState";
import LoadingState from "../ui/LoadingState";
import StatusTag from "../ui/StatusTag";
import ZplPrintHelpDialog from "../ui/ZplPrintHelpDialog";
import LabelPreview from "../label/LabelPreview";
import { CopyField, PlainField } from "../ui/DataFields";
import type { ToastItem } from "../ui/toasts";
import type { ApprovalLeg, QrResponse, Role } from "../../api/types";
import { brand } from "../../styles/brand";
import { formatDateDDMMYYYY } from "../../utils/dateFormat";
import { formatDateTime, LABELS } from "../../utils/displayLabels";
import { formatNumber } from "../../utils/formatNumber";
import { generateQrPlain } from "../../utils/qrWithLogo";
import { downloadAuditPdf } from "../../utils/downloadAuditPdf";
import { downloadLabelZplFile } from "../../utils/downloadLabelZpl";
import {
  canDownloadLabelPdf,
  canPrintLabel,
  parseEnvaseTotal,
  validateReprintRange,
} from "../../utils/labelPreviewPermissions";

const useStyles = makeStyles({
  dataGrid: {
    marginTop: "12px",
    display: "grid",
    gridTemplateColumns: "1fr 1fr",
    gap: "12px",
    "@media (max-width: 640px)": {
      gridTemplateColumns: "1fr",
    },
  },
  previewWrap: {
    display: "grid",
    placeItems: "center",
    backgroundColor: brand.surface,
    borderRadius: "12px",
    ...shorthands.padding("16px"),
    ...shorthands.border("1px", "solid", brand.border),
    overflowX: "auto",
  },
  actions: { display: "flex", gap: "10px", flexWrap: "wrap", marginTop: "12px" },
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

function asText(v: unknown, fallback = "—"): string {
  if (v === null || v === undefined) return fallback;
  if (typeof v === "string") return v.trim() ? v : fallback;
  if (typeof v === "number" || typeof v === "boolean") return String(v);
  return fallback;
}

function ApprovalLegBlock({
  title,
  approved,
  leg,
}: {
  title: string;
  approved: boolean;
  leg?: ApprovalLeg | null;
}) {
  if (!approved && !leg?.approved) {
    return (
      <div style={{ border: `1px solid ${brand.border}`, borderRadius: 10, padding: 10 }}>
        <Text weight="semibold">{title}</Text>
        <Text style={{ display: "block", marginTop: 4, color: brand.muted }}>Pendiente</Text>
      </div>
    );
  }
  const when = formatDateTime(leg?.at ?? null);
  const who = (leg?.actorEmail ?? "").trim() || "—";
  return (
    <div style={{ border: `1px solid ${brand.border}`, borderRadius: 10, padding: 10 }}>
      <Text weight="semibold">{title}</Text>
      <Text style={{ display: "block", marginTop: 4 }}>Aprobada</Text>
      <Text style={{ display: "block", marginTop: 4, color: brand.muted, fontSize: 12 }}>
        {who} · {when.date} {when.time}
      </Text>
    </div>
  );
}

function needsCalidadApproval(tipo: string): boolean {
  const t = (tipo || "").toUpperCase();
  return t.includes("MATERIA_PRIMA") || t.includes("EMPAQUE_PRIMARIO") || t === "MP";
}

type SharedProps = {
  platform: QrResponse | null;
  platformLoading: boolean;
  hasRole: (r: Role) => boolean;
  onToast: (t: ToastItem) => void;
};

export function PlatformInfoPanel({ platform, platformLoading, onToast }: SharedProps) {
  const s = useStyles();

  if (platformLoading) {
    return (
      <AppCard>
        <LoadingState label="Cargando información de la plataforma…" />
      </AppCard>
    );
  }

  if (!platform?.label) {
    return (
      <AppCard>
        <EmptyState
          title="Aún no existe una etiqueta para este lote"
          hint="Cuando se registre una etiqueta en Olnatura QR, aquí verás los datos de la plataforma."
        />
      </AppCard>
    );
  }

  const label = platform.label;
  const platformStatus = String(platform.dynamic?.platformStatus ?? "").trim().toUpperCase();
  const tipoMaterialDisplay =
    platform.permissions?.tipoMaterialDisplay ?? asText(label.tipoMaterial);
  const tipoMaterialCode = String(label.tipoMaterial ?? "").trim();
  const pendingMessage = platform.permissions?.pendingMessage ?? null;
  const calidadApproved = !!platform.permissions?.calidadApproved;
  const calidadLeg = platform.permissions?.calidad;
  const inspeccionApproved = !!platform.permissions?.inspeccionApproved;
  const inspeccionLeg = platform.permissions?.inspeccion;

  const envase =
    label.envaseNum == null && label.envaseTotal == null
      ? "—"
      : `${formatNumber(label.envaseNum)} / ${formatNumber(label.envaseTotal)}`;

  const fechaEntrada = label.fechaEntrada
    ? formatDateDDMMYYYY(String(label.fechaEntrada)) || asText(label.fechaEntrada)
    : "—";
  const caducidad = label.caducidad
    ? formatDateDDMMYYYY(String(label.caducidad)) || asText(label.caducidad)
    : "—";
  const reanalisis = label.reanalisis
    ? formatDateDDMMYYYY(String(label.reanalisis)) || asText(label.reanalisis)
    : "—";

  const handleCopy = async (fieldLabel: string, value: string) => {
    const v = (value ?? "").toString().trim();
    if (!v) return;
    try {
      await navigator.clipboard.writeText(v);
      onToast({
        intent: "success",
        title: "Copiado",
        message: `${fieldLabel} copiado al portapapeles.`,
      });
    } catch {
      onToast({
        intent: "error",
        title: "No se pudo copiar",
        message: "Intenta de nuevo o copia manualmente.",
      });
    }
  };

  return (
    <AppCard>
      <div style={{ display: "flex", alignItems: "center", gap: 10, flexWrap: "wrap", marginBottom: 4 }}>
        <Text weight="semibold">{LABELS.labelData}</Text>
        <Text style={{ fontSize: 12, color: brand.muted }}>Etiqueta registrada en Olnatura QR</Text>
      </div>

      {platformStatus ? (
        <div style={{ display: "flex", alignItems: "center", gap: 10, flexWrap: "wrap", marginTop: 8 }}>
          <Text style={{ fontSize: 12, color: brand.muted }}>{LABELS.platformStatus}</Text>
          <StatusTag status={platformStatus} />
        </div>
      ) : null}

      <div className={s.dataGrid}>
        <PlainField label="Tipo material" value={tipoMaterialDisplay} />
        <PlainField label="Nombre" value={asText(label.nombre)} />
        <CopyField
          label="Código"
          value={asText(label.codigo)}
          onCopy={handleCopy}
          boxed={false}
        />
        <CopyField
          label="Lote"
          value={asText(label.lote)}
          onCopy={handleCopy}
          boxed={false}
        />
        <PlainField label="Token QR" value={asText(label.publicToken)} />
        <PlainField label="Fecha entrada" value={fechaEntrada} />
        <PlainField label="Caducidad" value={caducidad} />
        <PlainField label="Reanálisis" value={reanalisis} />
        <PlainField label={LABELS.envase} value={envase} />
        <PlainField
          label="Cantidad por envase"
          value={formatNumber(asText(label.cantidadPorEnvase))}
        />
      </div>

      {(needsCalidadApproval(tipoMaterialCode) || pendingMessage || inspeccionApproved || calidadApproved) && (
        <div style={{ marginTop: 14, fontSize: 13, color: brand.text2, display: "grid", gap: 8 }}>
          <Text weight="semibold" style={{ fontSize: 13 }}>
            {LABELS.platformWorkflow}
          </Text>
          {needsCalidadApproval(tipoMaterialCode) ? (
            <ApprovalLegBlock title="Calidad" approved={calidadApproved} leg={calidadLeg} />
          ) : null}
          {inspeccionApproved || inspeccionLeg ? (
            <ApprovalLegBlock title="Inspección" approved={inspeccionApproved} leg={inspeccionLeg} />
          ) : null}
          {pendingMessage ? (
            <Text style={{ color: brand.warningFg, fontWeight: 600 }}>{pendingMessage}</Text>
          ) : null}
        </div>
      )}
    </AppCard>
  );
}

export function LabelPreviewPanel({
  platform,
  platformLoading,
  hasRole,
  onToast,
}: SharedProps) {
  const s = useStyles();
  const previewRef = useRef<HTMLDivElement>(null);
  const [qrDataUrl, setQrDataUrl] = useState<string | null>(null);
  const [qrBusy, setQrBusy] = useState(false);
  const [zplBusy, setZplBusy] = useState(false);
  const [zplHelpOpen, setZplHelpOpen] = useState(false);
  const [printFrom, setPrintFrom] = useState("1");
  const [printTo, setPrintTo] = useState("1");

  const label = platform?.label ?? null;
  const allowPdf = canDownloadLabelPdf(hasRole);
  const allowPrint = canPrintLabel(hasRole);
  const envaseTotal = useMemo(() => parseEnvaseTotal(label), [label]);
  const loteKey = String(label?.lote ?? "").trim();

  useEffect(() => {
    let cancelled = false;
    async function buildQr() {
      if (!label) {
        setQrDataUrl(null);
        return;
      }
      setQrBusy(true);
      try {
        const payload = label.publicToken
          ? `OLNQR:1:${label.publicToken}`
          : String(label.lote ?? "");
        const url = await generateQrPlain(payload, { width: 220, margin: 2 });
        if (!cancelled) {
          setQrDataUrl(url);
          setPrintFrom("1");
          setPrintTo(String(parseEnvaseTotal(label)));
        }
      } catch {
        if (!cancelled) setQrDataUrl(null);
      } finally {
        if (!cancelled) setQrBusy(false);
      }
    }
    void buildQr();
    return () => {
      cancelled = true;
    };
  }, [label]);

  if (platformLoading) {
    return (
      <AppCard>
        <LoadingState label="Cargando vista previa…" />
      </AppCard>
    );
  }

  if (!label) {
    return (
      <AppCard>
        <EmptyState
          title="Sin vista previa de etiqueta"
          hint="Registra una etiqueta para este lote para ver la vista previa aquí."
        />
      </AppCard>
    );
  }

  async function reprintZpl() {
    if (!label || !allowPrint) return;
    const validated = validateReprintRange(printFrom, printTo, envaseTotal, formatNumber);
    if (!validated.ok) {
      onToast({ intent: "error", title: "Rango inválido", message: validated.message });
      return;
    }
    setZplBusy(true);
    try {
      const key = String(label.id ?? label.lote ?? loteKey).trim();
      await downloadLabelZplFile({
        labelIdOrLote: key,
        totalEnvases: envaseTotal,
        printFrom: validated.from,
        printTo: validated.to,
      });
      onToast({
        intent: "success",
        title: "Etiqueta descargada",
        message: "Archivo Zebra (.zpl) listo para imprimir.",
      });
    } catch (e) {
      onToast({
        intent: "error",
        title: "No se pudo imprimir",
        message:
          (e as Error)?.message?.trim() ||
          "No se pudo descargar la etiqueta Zebra (.zpl).",
      });
    } finally {
      setZplBusy(false);
    }
  }

  function handleDownloadPdf() {
    if (!allowPdf || !loteKey) return;
    void downloadAuditPdf(loteKey, (msg) =>
      onToast({ intent: "error", title: "Error al descargar PDF", message: msg })
    );
  }

  const hasPreview = !!(label && qrDataUrl);

  return (
    <AppCard>
      <Text weight="semibold" style={{ display: "block", marginBottom: 8 }}>
        Vista previa
      </Text>

      <div ref={previewRef} className={s.previewWrap}>
        {qrBusy && !hasPreview ? (
          <LoadingState label="Generando QR…" />
        ) : hasPreview ? (
          <div style={{ width: 400, height: 300, overflow: "hidden" }}>
            <div
              style={{
                width: 800,
                height: 600,
                transform: "scale(0.5)",
                transformOrigin: "top left",
              }}
            >
              <LabelPreview
                materialName={String(label.nombre ?? "").trim() || "—"}
                codigo={String(label.codigo ?? "").trim() || "—"}
                lote={String(label.lote ?? "").trim() || "—"}
                fecha={label.fechaEntrada ?? "N/A"}
                caducidad={
                  (label as { fechaTipo?: string }).fechaTipo === "REANALISIS"
                    ? ""
                    : String(
                        (label as { fechaValor?: string }).fechaValor ?? label.caducidad ?? ""
                      )
                }
                reanalisis={
                  (label as { fechaTipo?: string }).fechaTipo === "REANALISIS"
                    ? String(
                        (label as { fechaValor?: string }).fechaValor ?? label.reanalisis ?? ""
                      )
                    : String(label.reanalisis ?? "")
                }
                cantidad={String(label.cantidadPorEnvase ?? "").trim() || "N/A"}
                envaseNum={label.envaseNum ?? "—"}
                envaseTotal={label.envaseTotal ?? "—"}
                qrData={qrDataUrl}
                logoUrl={`${import.meta.env.BASE_URL}logo-olnatura.png`}
                documentCode={
                  (label as { documentCode?: string }).documentCode ?? "AL-001-E02/04"
                }
              />
            </div>
          </div>
        ) : (
          <Text style={{ opacity: 0.6 }}>Sin vista previa</Text>
        )}
      </div>

      {(allowPdf || allowPrint) && (
        <div className={s.actions}>
          {allowPdf ? (
            <Button appearance="secondary" onClick={handleDownloadPdf}>
              {LABELS.downloadAuditPdf}
            </Button>
          ) : null}
        </div>
      )}

      {allowPrint ? (
        <div className={s.printBox}>
          <Text weight="semibold">Imprimir etiqueta</Text>
          <Text style={{ fontSize: 12, color: brand.muted }}>
            Este lote tiene <strong>{formatNumber(envaseTotal)}</strong> envase(s) registrado(s).
            Solo puedes imprimir del 1 al {formatNumber(envaseTotal)}.
          </Text>
          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 8 }}>
            <div>
              <Text style={{ fontSize: 12, color: brand.muted }}>Desde</Text>
              <Input
                type="number"
                min={1}
                max={envaseTotal}
                value={printFrom}
                onChange={(_, d) => setPrintFrom(d.value ?? "")}
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
                onChange={(_, d) => setPrintTo(d.value ?? "")}
                disabled={zplBusy}
              />
            </div>
          </div>
          <div className={s.actions} style={{ marginTop: 0 }}>
            <Button
              appearance="primary"
              disabled={zplBusy || qrBusy || !hasPreview}
              onClick={() => void reprintZpl()}
            >
              {zplBusy ? "Descargando…" : LABELS.downloadZpl}
            </Button>
            <Link onClick={() => setZplHelpOpen(true)} style={{ alignSelf: "center", fontSize: 13 }}>
              Cómo imprimir
            </Link>
          </div>
        </div>
      ) : null}

      <ZplPrintHelpDialog open={zplHelpOpen} onOpenChange={setZplHelpOpen} />
    </AppCard>
  );
}
