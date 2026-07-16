import * as React from "react";
import { Link, Text } from "@fluentui/react-components";
import { brand } from "../../styles/brand";
import { formatAuditDetail } from "../../utils/displayLabels";

const TECHNICAL_LABELS = new Set(["ID de etiqueta", "ID de usuario", "ID de usuario destino"]);

export default function AuditDetailCell({
  metadata,
  deviceId,
}: {
  metadata?: Record<string, unknown> | string | null;
  deviceId?: string | null;
}) {
  const [showTech, setShowTech] = React.useState(false);
  const entries = formatAuditDetail(metadata, deviceId);
  const main = entries.filter((e) => !TECHNICAL_LABELS.has(e.label));
  const tech = entries.filter((e) => TECHNICAL_LABELS.has(e.label));

  if (entries.length === 0) return <span style={{ color: brand.muted }}>—</span>;

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 6, minWidth: 160 }}>
      {main.map(({ label, value }) => (
        <div
          key={label}
          style={{
            display: "flex",
            flexWrap: "wrap",
            gap: "4px 8px",
            fontSize: 13,
            lineHeight: 1.45,
          }}
        >
          <span style={{ fontWeight: 600, color: brand.muted }}>{label}:</span>
          <span style={{ color: brand.text }}>{value}</span>
        </div>
      ))}
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
          {showTech ? (
            <div style={{ display: "flex", flexDirection: "column", gap: 4, marginTop: 6 }}>
              {tech.map(({ label, value }) => (
                <div key={label} style={{ fontSize: 12, lineHeight: 1.4, color: brand.muted }}>
                  <Text weight="semibold" size={200} style={{ color: brand.muted }}>
                    {label}:{" "}
                  </Text>
                  <span style={{ wordBreak: "break-all" }}>{value}</span>
                </div>
              ))}
            </div>
          ) : null}
        </div>
      ) : null}
    </div>
  );
}
