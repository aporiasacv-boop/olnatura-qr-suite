import * as React from "react";
import { Link, Text } from "@fluentui/react-components";
import { brand } from "../../styles/brand";
import { formatAuditDetail } from "../../utils/displayLabels";
import { formatNumber } from "../../utils/formatNumber";

const TECHNICAL_LABELS = new Set(["ID de etiqueta", "ID de usuario", "ID de usuario destino"]);

const COLLAPSE_ACTION_TYPES = new Set([
  "EXPORT_EXECUTIVE_DASHBOARD",
  "EXPORT_AUDIT_CSV",
  "EXPORT_AUDIT_PDF",
  "PRINT_LABEL",
]);

function parseMeta(metadata?: Record<string, unknown> | string | null): Record<string, unknown> | null {
  if (metadata == null) return null;
  if (typeof metadata === "object" && !Array.isArray(metadata)) return metadata;
  if (typeof metadata === "string") {
    try {
      const p = JSON.parse(metadata);
      return p && typeof p === "object" && !Array.isArray(p) ? p : null;
    } catch {
      return null;
    }
  }
  return null;
}

function detailSummary(
  actionType: string | undefined,
  metadata?: Record<string, unknown> | string | null
): string | null {
  const meta = parseMeta(metadata);
  if (!meta) return null;
  const at = (actionType || "").toUpperCase();

  if (at === "EXPORT_EXECUTIVE_DASHBOARD") {
    const parts: string[] = [];
    if (meta.labelsExported != null) parts.push(`${formatNumber(meta.labelsExported as number)} etiquetas`);
    if (meta.scansExported != null) parts.push(`${formatNumber(meta.scansExported as number)} escaneos`);
    if (meta.auditsExported != null) parts.push(`${formatNumber(meta.auditsExported as number)} auditorías`);
    if (meta.usersExported != null) parts.push(`${formatNumber(meta.usersExported as number)} usuarios`);
    return parts.length > 0 ? parts.join(" · ") : "Excel Power BI";
  }

  if (at === "EXPORT_AUDIT_CSV" || at === "EXPORT_AUDIT_PDF") {
    if (meta.countEvents != null) return `${formatNumber(meta.countEvents as number)} eventos`;
    if (meta.filename != null) return String(meta.filename);
  }

  if (at === "PRINT_LABEL") {
    const parts: string[] = [];
    const mode = meta.mode != null ? String(meta.mode).toUpperCase() : "";
    if (mode === "ZPL_DOWNLOAD") parts.push("Descarga ZPL");
    else if (meta.mode != null) parts.push(String(meta.mode));

    if (meta.count != null) parts.push(`${formatNumber(meta.count as number)} etiquetas`);
    else if (meta.from != null && meta.to != null) {
      parts.push(`${formatNumber(meta.from as number)}–${formatNumber(meta.to as number)}`);
    } else if (meta.from != null) {
      parts.push(`desde ${formatNumber(meta.from as number)}`);
    }

    if (meta.lote != null && String(meta.lote).trim()) {
      parts.push(String(meta.lote).trim());
    }
    return parts.length > 0 ? parts.join(" · ") : "Impresión etiqueta";
  }

  return null;
}

function EntryList({
  entries,
  muted,
}: {
  entries: { label: string; value: string }[];
  muted?: boolean;
}) {
  return (
    <div style={{ display: "flex", flexDirection: "column", gap: muted ? 4 : 6 }}>
      {entries.map(({ label, value }) => (
        <div
          key={label}
          style={{
            display: "flex",
            flexWrap: "wrap",
            gap: "4px 8px",
            fontSize: muted ? 12 : 13,
            lineHeight: 1.45,
            color: muted ? brand.muted : undefined,
          }}
        >
          <span style={{ fontWeight: 600, color: brand.muted }}>{label}:</span>
          <span style={{ color: muted ? brand.muted : brand.text, wordBreak: "break-word" }}>
            {value}
          </span>
        </div>
      ))}
    </div>
  );
}

export default function AuditDetailCell({
  metadata,
  actionType,
}: {
  metadata?: Record<string, unknown> | string | null;
  actionType?: string | null;
}) {
  const [showTech, setShowTech] = React.useState(false);
  const [expanded, setExpanded] = React.useState(false);
  const entries = formatAuditDetail(metadata);
  const main = entries.filter((e) => !TECHNICAL_LABELS.has(e.label));
  const tech = entries.filter((e) => TECHNICAL_LABELS.has(e.label));

  const shouldCollapse =
    COLLAPSE_ACTION_TYPES.has((actionType || "").toUpperCase()) || main.length > 3;
  const summary = detailSummary(actionType || undefined, metadata);

  if (entries.length === 0) return <span style={{ color: brand.muted }}>—</span>;

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 6, minWidth: 160 }}>
      {shouldCollapse && !expanded ? (
        <>
          {summary ? (
            <Text size={300} style={{ color: brand.text, lineHeight: 1.4 }}>
              {summary}
            </Text>
          ) : (
            <Text size={300} style={{ color: brand.muted }}>
              {main.length} dato{main.length === 1 ? "" : "s"} de detalle
            </Text>
          )}
          <Link
            inline
            onClick={(e) => {
              e.preventDefault();
              setExpanded(true);
            }}
            style={{ fontSize: 12 }}
          >
            Ver detalle
          </Link>
        </>
      ) : (
        <>
          <EntryList entries={main} />
          {shouldCollapse ? (
            <Link
              inline
              onClick={(e) => {
                e.preventDefault();
                setExpanded(false);
              }}
              style={{ fontSize: 12 }}
            >
              Ocultar detalle
            </Link>
          ) : null}
        </>
      )}

      {tech.length > 0 ? (
        <div style={{ marginTop: 2 }}>
          <Link
            inline
            onClick={(e) => {
              e.preventDefault();
              setShowTech((v) => !v);
            }}
            style={{ fontSize: 12 }}
          >
            {showTech ? "Ocultar IDs técnicos" : "Ver IDs técnicos"}
          </Link>
          {showTech ? <EntryList entries={tech} muted /> : null}
        </div>
      ) : null}
    </div>
  );
}
