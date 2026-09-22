import type { CSSProperties, ReactNode } from "react";
import { formatDateLabelDDMMMYY } from "../../utils/dateFormat";
import { resolveLabelDocumentCode } from "../../utils/labelDocumentCode";
import { labelHeaderTitle } from "../../utils/labelHeaderTitle";

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
  logoUrl?: string;
  documentCode?: string;
  tipoMaterial?: string | null;
  cantidadTotal?: string;
};

const LABEL_WIDTH = 800;
const LABEL_HEIGHT = 600;
const BORDER = "2px solid #000";
const FONT = "Arial, Helvetica, sans-serif";

const DEFAULT_LOGO = "/logo-olnatura.png";
const FOOTER_COMPLIANCE =
  "Propiedad de Olnatura S.A. de C.V. Prohibido su uso, divulgacion y/o reproduccion total o parcial. Si este documento no se encuentra controlado, se considera COPIA SOLO PARA INFORMACION.";

function pad2(v: number | string): string {
  const n = Number(String(v ?? "").trim());
  if (!Number.isFinite(n) || n < 0) return String(v ?? "").trim() || "00";
  return String(Math.trunc(n)).padStart(2, "0");
}

function Cell({
  children,
  style,
}: {
  children: ReactNode;
  style?: CSSProperties;
}) {
  return (
    <div
      style={{
        boxSizing: "border-box",
        borderRight: BORDER,
        borderBottom: BORDER,
        padding: "6px 10px",
        minHeight: 0,
        overflow: "hidden",
        ...style,
      }}
    >
      {children}
    </div>
  );
}

function Field({
  label,
  value,
  valueSize = 20,
  labelSize = 13,
}: {
  label: string;
  value: string;
  valueSize?: number;
  labelSize?: number;
}) {
  const heading = label.endsWith(":") ? label : `${label}:`;
  return (
    <>
      <div
        style={{
          fontSize: labelSize,
          fontWeight: 700,
          lineHeight: 1.15,
          marginBottom: 4,
          overflowWrap: "anywhere",
          wordBreak: "break-word",
        }}
      >
        {heading}
      </div>
      <div
        style={{
          fontSize: valueSize,
          fontWeight: 700,
          lineHeight: 1.1,
          wordBreak: "break-word",
        }}
      >
        {value}
      </div>
    </>
  );
}

export default function LabelPreview({
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
  logoUrl = DEFAULT_LOGO,
  documentCode,
  tipoMaterial,
}: LabelPreviewProps) {
  const fechaFmt = formatDateLabelDDMMMYY(fecha) || "N/A";
  const caducidadFmt = formatDateLabelDDMMMYY(caducidad);
  const reanalisisFmt = formatDateLabelDDMMMYY(reanalisis);

  const nombreStr = String(materialName ?? "").trim() || "N/A";
  const codigoStr = String(codigo ?? "").trim() || "N/A";
  const loteStr = String(lote ?? "").trim() || "N/A";
  const cantidadStr = String(cantidad ?? "").trim() || "N/A";
  const envaseNumStr = pad2(envaseNum);
  const envaseTotalStr = pad2(envaseTotal);
  const envaseDisplay = `${envaseNumStr} de ${envaseTotalStr}`;
  const totalEnvasesStr = String(envaseTotal ?? "").trim() || "0";
  const documentCodeResolved = resolveLabelDocumentCode(documentCode);
  const headerTitle = labelHeaderTitle(tipoMaterial);
  const nombreFont =
    nombreStr.length > 80 ? 14 : nombreStr.length > 40 ? 16 : 18;

  return (
    <div
      data-label-preview
      style={{
        width: LABEL_WIDTH,
        height: LABEL_HEIGHT,
        background: "#fff",
        border: BORDER,
        boxSizing: "border-box",
        fontFamily: FONT,
        color: "#000",
        overflow: "hidden",
        display: "grid",
        gridTemplateColumns: "90px 1fr 1fr 1fr 400px",
        gridTemplateRows: "50px 50px 65px 70px 70px 70px 90px 95px",
      }}
    >
      <Cell
        style={{
          gridColumn: "1 / 2",
          gridRow: "1 / 3",
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          padding: 8,
          borderBottom: BORDER,
        }}
      >
        {logoUrl ? (
          <img
            src={logoUrl}
            alt="Logo"
            style={{ maxWidth: "100%", maxHeight: "100%", objectFit: "contain" }}
          />
        ) : null}
      </Cell>

      <Cell
        style={{
          gridColumn: "2 / 6",
          gridRow: 1,
          display: "flex",
          alignItems: "center",
          padding: "0 14px",
          borderRight: "none",
        }}
      >
        <div style={{ fontSize: 22, fontWeight: 700, letterSpacing: 0.4 }}>
          {headerTitle}
        </div>
      </Cell>

      <Cell
        style={{
          gridColumn: "2 / 6",
          gridRow: 2,
          display: "flex",
          alignItems: "center",
          padding: "2px 14px",
          borderRight: "none",
        }}
      >
        <div
          style={{
            fontSize: nombreFont,
            fontWeight: 700,
            lineHeight: 1.15,
            width: "100%",
            whiteSpace: "normal",
            overflowWrap: "anywhere",
            wordBreak: "break-word",
          }}
        >
          {`Nombre: ${nombreStr}`}
        </div>
      </Cell>

      <Cell style={{ gridColumn: "1 / 2", gridRow: 3 }}>
        <Field label="Fecha:" value={fechaFmt} valueSize={16} />
      </Cell>
      <Cell style={{ gridColumn: "2 / 4", gridRow: 3 }}>
        <Field label="Código:" value={codigoStr} valueSize={16} />
      </Cell>
      <Cell style={{ gridColumn: "4 / 6", gridRow: 3, borderRight: "none" }}>
        <Field label="Lote:" value={loteStr} valueSize={15} />
      </Cell>

      <Cell style={{ gridColumn: "1 / 4", gridRow: 4 }}>
        <Field label="Fecha de Caducidad:" value={caducidadFmt} />
      </Cell>
      <Cell style={{ gridColumn: "1 / 4", gridRow: 5 }}>
        <Field label="Fecha de Reanálisis:" value={reanalisisFmt} />
      </Cell>
      <Cell style={{ gridColumn: "1 / 4", gridRow: 6 }}>
        <Field label="Cantidad por envase:" value={cantidadStr} />
      </Cell>

      <Cell
        style={{
          gridColumn: "4 / 6",
          gridRow: "4 / 8",
          borderRight: "none",
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          padding: 10,
        }}
      >
        {qrData ? (
          <img
            src={qrData}
            alt="QR"
            style={{
              width: 260,
              height: 260,
              objectFit: "contain",
              display: "block",
            }}
          />
        ) : (
          <div
            style={{
              width: 260,
              height: 260,
              border: BORDER,
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              fontSize: 22,
              fontWeight: 700,
            }}
          >
            QR
          </div>
        )}
      </Cell>

      <Cell style={{ gridColumn: "1 / 3", gridRow: 7 }}>
        <Field label="No. de envases:" value={envaseDisplay} valueSize={22} />
      </Cell>
      <Cell style={{ gridColumn: "3 / 4", gridRow: 7, padding: "6px 8px" }}>
        <Field label="Total de envases:" value={totalEnvasesStr} valueSize={22} labelSize={11} />
      </Cell>

      <Cell
        style={{
          gridColumn: "1 / 6",
          gridRow: 8,
          borderRight: "none",
          borderBottom: "none",
          padding: "10px 12px",
          fontSize: 11,
          lineHeight: 1.35,
          fontWeight: 500,
        }}
      >
        {documentCodeResolved} {FOOTER_COMPLIANCE}
      </Cell>
    </div>
  );
}
