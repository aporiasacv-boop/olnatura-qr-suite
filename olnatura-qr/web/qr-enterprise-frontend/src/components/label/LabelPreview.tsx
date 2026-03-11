/**
 * LabelPreview - Renders the label exactly as it will print.
 * Matches warehouse label layout. Updates live when props change.
 *
 * Layout:
 * HEADER: "MATERIAL DE ACONDICIONADO"
 * SECTION: Nombre
 * ROW: Fecha | Código | Lote
 * SECTION: Caducidad, Reanálisis, Cantidad
 * BOTTOM ROW: Envase No | Cantidad total
 * RIGHT SIDE: QR with Olnatura logo
 */

import { useRef } from "react";
import { formatDateDDMMYYYY } from "../../utils/dateFormat";

export type LabelPreviewProps = {
  materialName: string;
  codigo: string;
  lote: string;
  fecha: string;
  caducidad: string;
  reanalisis: string;
  cantidad: string;
  envaseNum: number | string;
  envaseTotal: number | string;
  qrData: string | null;
};

export default function LabelPreview(props: LabelPreviewProps) {
  const {
    materialName,
    codigo,
    lote,
    fecha,
    caducidad,
    reanalisis,
    cantidad,
    envaseNum,
    envaseTotal,
    qrData,
  } = props;

  const rootRef = useRef<HTMLDivElement>(null);

  const fechaFmt = formatDateDDMMYYYY(fecha);
  const caducidadFmt = formatDateDDMMYYYY(caducidad);
  const reanalisisFmt = formatDateDDMMYYYY(reanalisis);

  const envaseNumStr = String(envaseNum ?? "").trim() || "—";
  const envaseTotalStr = String(envaseTotal ?? "").trim() || "—";

  return (
    <div
      ref={rootRef}
      className="label-preview-root"
      style={labelStyles.root}
      data-label-preview
    >
      <div style={labelStyles.border}>
        {/* HEADER */}
        <div style={labelStyles.header}>
          MATERIAL DE ACONDICIONADO
        </div>

        {/* SECTION: Nombre */}
        <div style={labelStyles.section}>
          <div style={labelStyles.sectionLabel}>Nombre</div>
          <div style={labelStyles.sectionValue}>{materialName || "—"}</div>
        </div>

        {/* ROW: Fecha | Código | Lote */}
        <div style={labelStyles.row3}>
          <div style={labelStyles.cell}>
            <div style={labelStyles.cellLabel}>Fecha</div>
            <div style={labelStyles.cellValue}>{fechaFmt || "—"}</div>
          </div>
          <div style={labelStyles.cell}>
            <div style={labelStyles.cellLabel}>Código</div>
            <div style={labelStyles.cellValue}>{codigo || "—"}</div>
          </div>
          <div style={labelStyles.cell}>
            <div style={labelStyles.cellLabel}>Lote</div>
            <div style={labelStyles.cellValue}>{lote || "—"}</div>
          </div>
        </div>

        {/* SECTION: Caducidad, Reanálisis, Cantidad */}
        <div style={labelStyles.sectionGrid}>
          <div style={labelStyles.gridRow}>
            <span style={labelStyles.gridLabel}>Caducidad</span>
            <span style={labelStyles.gridValue}>{caducidadFmt || "—"}</span>
          </div>
          <div style={labelStyles.gridRow}>
            <span style={labelStyles.gridLabel}>Reanálisis</span>
            <span style={labelStyles.gridValue}>{reanalisisFmt || "—"}</span>
          </div>
          <div style={labelStyles.gridRow}>
            <span style={labelStyles.gridLabel}>Cantidad</span>
            <span style={labelStyles.gridValue}>{cantidad || "—"}</span>
          </div>
        </div>

        {/* BOTTOM ROW: Envase No | Cantidad total */}
        <div style={labelStyles.bottomRow}>
          <div style={labelStyles.envaseBlock}>
            <div style={labelStyles.envaseLabel}>Envase No</div>
            <div style={labelStyles.envaseNum}>{envaseNumStr}</div>
            <span style={labelStyles.envaseDe}>de</span>
            <div style={labelStyles.envaseTotal}>{envaseTotalStr}</div>
          </div>
          <div style={labelStyles.totalBlock}>
            <div style={labelStyles.totalLabel}>Cantidad total</div>
            <div style={labelStyles.totalValue}>{envaseTotalStr}</div>
          </div>
        </div>

        {/* RIGHT SIDE: QR with logo */}
        <div style={labelStyles.qrArea}>
          {qrData ? (
            <img
              src={qrData}
              alt="QR"
              style={labelStyles.qrImg}
            />
          ) : (
            <div style={labelStyles.qrPlaceholder}>QR</div>
          )}
        </div>
      </div>
    </div>
  );
}

/**
 * Proportions match real printed label (Zebra 800×600 dots).
 * Scale: 1px ≈ 2 dots for consistent visual match.
 */
const LABEL_WIDTH = 400;
const LABEL_HEIGHT = 300;
const QR_SIZE = 160;

const labelStyles: Record<string, React.CSSProperties> = {
  root: {
    width: LABEL_WIDTH,
    minHeight: LABEL_HEIGHT,
    fontFamily: "system-ui, 'Segoe UI', sans-serif",
    fontSize: 12,
  },
  border: {
    position: "relative",
    width: "100%",
    minHeight: LABEL_HEIGHT,
    backgroundColor: "#fff",
    border: "2px solid #45a350",
    borderRadius: 4,
    padding: 14,
    paddingRight: QR_SIZE + 18,
    boxSizing: "border-box",
  },
  header: {
    textAlign: "center",
    fontSize: 18,
    fontWeight: 700,
    color: "#1a1a1a",
    marginBottom: 10,
  },
  section: {
    marginBottom: 8,
  },
  sectionLabel: {
    fontSize: 11,
    color: "#6B7280",
    marginBottom: 2,
  },
  sectionValue: {
    fontSize: 14,
    fontWeight: 600,
    color: "#111827",
  },
  row3: {
    display: "grid",
    gridTemplateColumns: "1fr 1fr 1fr",
    gap: 8,
    marginBottom: 10,
  },
  cell: {
    padding: 6,
    border: "1px solid #E5E7EB",
    borderRadius: 2,
  },
  cellLabel: {
    fontSize: 10,
    color: "#6B7280",
  },
  cellValue: {
    fontSize: 12,
    fontWeight: 600,
  },
  sectionGrid: {
    display: "grid",
    gap: 4,
    marginBottom: 12,
  },
  gridRow: {
    display: "flex",
    gap: 12,
  },
  gridLabel: {
    fontSize: 11,
    color: "#6B7280",
    minWidth: 70,
  },
  gridValue: {
    fontSize: 12,
    fontWeight: 600,
  },
  bottomRow: {
    display: "flex",
    gap: 16,
    alignItems: "flex-end",
  },
  envaseBlock: {
    display: "flex",
    alignItems: "baseline",
    gap: 6,
  },
  envaseLabel: {
    fontSize: 11,
    color: "#6B7280",
  },
  envaseNum: {
    fontSize: 24,
    fontWeight: 700,
  },
  envaseDe: {
    fontSize: 12,
    color: "#6B7280",
  },
  envaseTotal: {
    fontSize: 24,
    fontWeight: 700,
  },
  totalBlock: {},
  totalLabel: {
    fontSize: 11,
    color: "#6B7280",
  },
  totalValue: {
    fontSize: 22,
    fontWeight: 700,
  },
  qrArea: {
    position: "absolute",
    right: 12,
    top: 12,
    width: QR_SIZE,
    height: QR_SIZE,
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
  },
  qrImg: {
    width: QR_SIZE,
    height: QR_SIZE,
    objectFit: "contain",
  },
  qrPlaceholder: {
    width: QR_SIZE,
    height: QR_SIZE,
    backgroundColor: "#F3F4F6",
    borderRadius: 4,
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    color: "#9CA3AF",
    fontSize: 14,
  },
};
