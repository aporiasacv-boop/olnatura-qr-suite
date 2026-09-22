import { useEffect, useState } from "react";
import { Button, Text, makeStyles, shorthands } from "@fluentui/react-components";
import EmptyState from "../ui/EmptyState";
import LoadingState from "../ui/LoadingState";
import StatusTag from "../ui/StatusTag";
import LabelPreview from "../label/LabelPreview";
import { PlainField } from "../ui/DataFields";
import type { ToastItem } from "../ui/toasts";
import type { QrResponse, Role } from "../../api/types";
import { formatDateDDMMYYYY } from "../../utils/dateFormat";
import { LABELS } from "../../utils/displayLabels";
import { formatNumber } from "../../utils/formatNumber";
import { generateQrPlain } from "../../utils/qrWithLogo";
import { downloadAuditPdf } from "../../utils/downloadAuditPdf";
import { canDownloadAuditPdf } from "../../utils/labelPreviewPermissions";
import { cantidadForEnvase, cantidadTotalOf, cantidadesMenoresOf, isRestosEnabled } from "../../utils/envaseRestos";
import { resolveLabelDocumentCode } from "../../utils/labelDocumentCode";

const useStyles = makeStyles({
  dataGrid: {
    display: "grid",
    gridTemplateColumns: "1fr",
    gap: "14px",
  },
  panel: {
    display: "grid",
    gap: "14px",
    alignContent: "start",
    backgroundColor: "#E6EBE3",
    ...shorthands.borderRadius("12px"),
    ...shorthands.padding("16px", "18px"),
    ...shorthands.border("1px", "solid", "#D5DCCF"),
  },
  previewWrap: {
    display: "grid",
    placeItems: "center",
    backgroundColor: "#E8EDF0",
    borderRadius: "10px",
    ...shorthands.padding("16px"),
    ...shorthands.border("1px", "solid", "#D5DCCF"),
    overflowX: "auto",
  },
  actions: { display: "flex", gap: "10px", flexWrap: "wrap", marginTop: "12px" },
  statusRow: {
    display: "grid",
    gap: "3px",
  },
  statusLabel: {
    fontSize: "12px",
    color: "#5A6570",
    fontWeight: 500,
    lineHeight: "1.3",
  },
});

function asText(v: unknown, fallback = "—"): string {
  if (v === null || v === undefined) return fallback;
  if (typeof v === "string") return v.trim() ? v : fallback;
  if (typeof v === "number" || typeof v === "boolean") return String(v);
  return fallback;
}

type SharedProps = {
  platform: QrResponse | null;
  platformLoading: boolean;
  hasRole: (r: Role) => boolean;
  onToast: (t: ToastItem) => void;
};

export function PlatformFieldsBlock({ platform, platformLoading }: SharedProps) {
  const s = useStyles();

  if (platformLoading) {
    return <LoadingState label="Cargando datos de plataforma…" />;
  }

  if (!platform?.label) {
    return (
      <Text weight="semibold" style={{ color: "#7A5A12" }}>
        No se ha generado etiqueta para este lote
      </Text>
    );
  }

  const label = platform.label;
  const platformStatus = String(platform.dynamic?.platformStatus ?? "").trim().toUpperCase();
  const tipoMaterialDisplay =
    platform.permissions?.tipoMaterialDisplay ?? asText(label.tipoMaterial);

  const envase =
    label.envaseNum == null && label.envaseTotal == null
      ? "—"
      : `${formatNumber(label.envaseNum)} / ${formatNumber(label.envaseTotal)}`;
  const cantidadesMenores = cantidadesMenoresOf(label);

  return (
    <div style={{ display: "grid", gap: 14 }}>
      {platformStatus ? (
        <div className={s.statusRow}>
          <div className={s.statusLabel}>{LABELS.platformStatus}</div>
          <StatusTag status={platformStatus} />
        </div>
      ) : null}

      <div className={s.dataGrid}>
        <PlainField label="Tipo material" value={tipoMaterialDisplay} boxed={false} />
        <PlainField label={LABELS.envase} value={envase} boxed={false} />
        <PlainField
          label="Cantidad por envase"
          value={formatNumber(asText(label.cantidadPorEnvase))}
          boxed={false}
        />
        {cantidadesMenores.map((qty, idx) => {
          const total = Number(label.envaseTotal);
          const envaseNum = Number.isFinite(total) ? total - cantidadesMenores.length + 1 + idx : idx + 1;
          return (
            <PlainField
              key={`menor-${envaseNum}-${qty}`}
              label={`Envase ${formatNumber(envaseNum)}`}
              value={formatNumber(qty)}
              boxed={false}
            />
          );
        })}
        <PlainField
          label="Reanálisis"
          value={
            label.reanalisis
              ? formatDateDDMMYYYY(String(label.reanalisis)) || asText(label.reanalisis)
              : "—"
          }
          boxed={false}
        />
      </div>
    </div>
  );
}

export function LabelPreviewPanel({
  platform,
  platformLoading,
  hasRole,
  onToast,
}: SharedProps) {
  const s = useStyles();
  const [qrDataUrl, setQrDataUrl] = useState<string | null>(null);
  const [qrBusy, setQrBusy] = useState(false);

  const label = platform?.label ?? null;
  const allowPdf = canDownloadAuditPdf(hasRole);
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
        if (!cancelled) setQrDataUrl(url);
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
      <div className={s.panel}>
        <LoadingState label="Cargando vista previa…" />
      </div>
    );
  }

  if (!label) {
    return (
      <div className={s.panel}>
        <EmptyState title="Sin vista previa" />
      </div>
    );
  }

  function handleDownloadPdf() {
    if (!allowPdf || !loteKey) return;
    void downloadAuditPdf(loteKey, (msg) =>
      onToast({ intent: "error", title: "Error al descargar PDF", message: msg })
    );
  }

  const hasPreview = !!(label && qrDataUrl);

  return (
    <div className={s.panel}>
      <div className={s.previewWrap}>
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
                cantidad={cantidadForEnvase(label, Number(label.envaseTotal) || 1)}
                envaseNum={
                  isRestosEnabled(label)
                    ? label.envaseTotal ?? "—"
                    : label.envaseNum ?? "—"
                }
                envaseTotal={label.envaseTotal ?? "—"}
                cantidadTotal={cantidadTotalOf(label)}
                qrData={qrDataUrl}
                logoUrl={`${import.meta.env.BASE_URL}logo-olnatura.png`}
                documentCode={resolveLabelDocumentCode(
                  (label as { documentCode?: string }).documentCode
                )}
                tipoMaterial={String(label.tipoMaterial ?? "").trim() || null}
              />
            </div>
          </div>
        ) : (
          <Text style={{ opacity: 0.6 }}>Sin vista previa</Text>
        )}
      </div>

      {allowPdf ? (
        <div className={s.actions}>
          <Button appearance="secondary" onClick={handleDownloadPdf}>
            {LABELS.downloadAuditPdf}
          </Button>
        </div>
      ) : null}
    </div>
  );
}
