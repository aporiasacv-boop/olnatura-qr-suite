/**
 * LabelPreview - warehouse label preview aligned to Zebra structure.
 * Fixed 800x600 layout, strong block borders, QR panel on right.
 */

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
  /** Logo en esquina (sin logo en QR). Ruta por defecto: /logo-olnatura.png */
  logoUrl?: string;
  documentCode?: string;
};

const LABEL_WIDTH = 800;
const LABEL_HEIGHT = 600;
const BORDER = "2px solid #000";
const FONT = "Arial, Helvetica, sans-serif";

const smallLabelStyle: React.CSSProperties = {
  fontSize: 16,
  fontWeight: 700,
  marginBottom: 8,
};

const valueStyle: React.CSSProperties = {
  fontSize: 24,
  fontWeight: 700,
  lineHeight: 1.15,
  wordBreak: "break-word",
};

const DEFAULT_LOGO = "/logo-olnatura.png";
const FOOTER_COMPLIANCE =
  "Propiedad de Olnatura S.A. de C.V. Prohibido su uso, divulgacion y/o reproduccion total o parcial. Si este documento no se encuentra controlado, se considera COPIA SOLO PARA INFORMACION.";

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
  documentCode = "AL-001-E02/04",
}: LabelPreviewProps) {
  const fechaFmt = formatDateDDMMYYYY(fecha) || "N/A";
  const caducidadFmt = formatDateDDMMYYYY(caducidad) || "N/A";
  const reanalisisFmt = formatDateDDMMYYYY(reanalisis) || "N/A";

  const nombreStr = String(materialName ?? "").trim() || "N/A";
  const codigoStr = String(codigo ?? "").trim() || "N/A";
  const loteStr = String(lote ?? "").trim() || "N/A";
  const cantidadStr = String(cantidad ?? "").trim() || "N/A";
  const envaseNumStr = String(envaseNum ?? "").trim() || "0";
  const envaseTotalStr = String(envaseTotal ?? "").trim() || "0";

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
        gridTemplateColumns: "520px 280px",
        /* Fila 5 ampliada: números grandes necesitan ~115px; antes 90px desbordaban sobre Caducidad */
        gridTemplateRows: "90px 85px 130px 125px 120px 50px",
      }}
    >
      {/* HEADER: logo esquina + titulo */}
      <div
        style={{
          gridColumn: "1 / 3",
          gridRow: 1,
          borderBottom: BORDER,
          display: "flex",
          alignItems: "stretch",
          minHeight: 0,
        }}
      >
        <div
          style={{
            width: 90,
            minWidth: 90,
            borderRight: BORDER,
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            padding: 8,
            boxSizing: "border-box",
          }}
        >
          {logoUrl && (
            <img
              src={logoUrl}
              alt="Logo"
              style={{ maxWidth: "100%", maxHeight: "100%", objectFit: "contain" }}
            />
          )}
        </div>
        <div
          style={{
            flex: 1,
            display: "flex",
            flexDirection: "column",
            justifyContent: "center",
            padding: "0 14px",
          }}
        >
          <div style={{ fontSize: 28, fontWeight: 700, letterSpacing: 0.5 }}>MATERIAL DE ACONDICIONADO</div>
          <div style={{ fontSize: 22, fontWeight: 700, marginTop: 4 }}>{nombreStr}</div>
        </div>
      </div>

      {/* QR: filas 2–4 (línea 5 = inicio pie, sin solaparse) */}
      <div
        style={{
          gridColumn: "2 / 3",
          gridRow: "2 / 5",
          borderLeft: BORDER,
          borderBottom: BORDER,
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          padding: 12,
          boxSizing: "border-box",
          background: "#fff",
        }}
      >
        {qrData ? (
          <img
            src={qrData}
            alt="QR"
            style={{
              width: 220,
              height: 220,
              objectFit: "contain",
              display: "block",
            }}
          />
        ) : (
          <div
            style={{
              width: 220,
              height: 220,
              border: BORDER,
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              fontSize: 24,
              fontWeight: 700,
            }}
          >
            QR
          </div>
        )}
      </div>

      {/* FECHA | CODIGO | LOTE */}
      <div
        style={{
          gridColumn: "1 / 2",
          gridRow: 2,
          display: "grid",
          gridTemplateColumns: "1fr 1fr 1fr",
          borderBottom: BORDER,
          minHeight: 0,
        }}
      >
        <div
          style={{
            padding: "10px 12px",
            borderRight: BORDER,
            boxSizing: "border-box",
          }}
        >
          <div style={smallLabelStyle}>Fecha:</div>
          <div style={{ ...valueStyle, fontSize: 18 }}>{fechaFmt}</div>
        </div>

        <div
          style={{
            padding: "10px 12px",
            borderRight: BORDER,
            boxSizing: "border-box",
          }}
        >
          <div style={smallLabelStyle}>Código:</div>
          <div style={{ ...valueStyle, fontSize: 18 }}>{codigoStr}</div>
        </div>

        <div
          style={{
            padding: "10px 12px",
            boxSizing: "border-box",
          }}
        >
          <div style={smallLabelStyle}>Lote:</div>
          <div style={{ ...valueStyle, fontSize: 16 }}>{loteStr}</div>
        </div>
      </div>

      {/* CADUCIDAD | REANALISIS | CANTIDAD */}
      <div
        style={{
          gridColumn: "1 / 2",
          gridRow: 3,
          borderBottom: BORDER,
          padding: "12px 16px",
          boxSizing: "border-box",
          display: "grid",
          gridTemplateColumns: "180px 1fr",
          rowGap: 14,
          alignContent: "start",
          minHeight: 0,
          overflow: "hidden",
        }}
      >
        <div style={{ fontSize: 20, fontWeight: 700 }}>Caducidad:</div>
        <div style={{ fontSize: 24, fontWeight: 700 }}>{caducidadFmt}</div>

        <div style={{ fontSize: 20, fontWeight: 700 }}>Reanálisis:</div>
        <div style={{ fontSize: 24, fontWeight: 700 }}>{reanalisisFmt}</div>

        <div style={{ fontSize: 20, fontWeight: 700 }}>Cantidad por envase:</div>
        <div style={{ fontSize: 24, fontWeight: 700 }}>{cantidadStr}</div>
      </div>

      {/* FOOTER: columna título + fila de valores (sin baseline que suba los números) */}
      <div
        style={{
          gridColumn: "1 / 3",
          gridRow: 5,
          display: "grid",
          gridTemplateColumns: "1fr 1fr",
          minHeight: 0,
          overflow: "hidden",
        }}
      >
        <div
          style={{
            borderRight: BORDER,
            padding: "8px 14px 10px",
            boxSizing: "border-box",
            display: "flex",
            flexDirection: "column",
            justifyContent: "flex-start",
            gap: 10,
            minHeight: 0,
          }}
        >
          <div style={{ fontSize: 18, fontWeight: 700, lineHeight: 1.2, flexShrink: 0 }}>
            No. de envases
          </div>
          <div
            style={{
              display: "flex",
              alignItems: "center",
              gap: 12,
              flexShrink: 0,
              lineHeight: 1,
            }}
          >
            <span style={{ fontSize: 46, fontWeight: 700, lineHeight: 1 }}>{envaseNumStr}</span>
            <span style={{ fontSize: 22, fontWeight: 700, lineHeight: 1 }}>de</span>
            <span style={{ fontSize: 46, fontWeight: 700, lineHeight: 1 }}>{envaseTotalStr}</span>
          </div>
        </div>

        <div
          style={{
            padding: "8px 14px 10px",
            boxSizing: "border-box",
            display: "flex",
            flexDirection: "column",
            justifyContent: "flex-start",
            gap: 10,
            minHeight: 0,
          }}
        >
          <div style={{ fontSize: 18, fontWeight: 700, lineHeight: 1.2, flexShrink: 0 }}>
            Cantidad total
          </div>
          <div style={{ fontSize: 52, fontWeight: 700, lineHeight: 1, flexShrink: 0 }}>
            {envaseTotalStr}
          </div>
        </div>
      </div>

      {/* FOOTER: documentCode + compliance */}
      <div
        style={{
          gridColumn: "1 / 3",
          gridRow: 6,
          borderTop: BORDER,
          padding: "10px 14px",
          fontSize: 12,
          lineHeight: 1.4,
          boxSizing: "border-box",
          minHeight: 0,
          overflow: "hidden",
        }}
      >
        {documentCode} {FOOTER_COMPLIANCE}
      </div>
    </div>
  );
}