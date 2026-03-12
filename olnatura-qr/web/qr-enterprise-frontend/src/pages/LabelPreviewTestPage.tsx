import { useEffect, useRef, useState } from "react";
import { Button, Card, Text } from "@fluentui/react-components";
import LabelPreview from "../components/label/LabelPreview";
import { generateQrWithLogo } from "../utils/qrWithLogo";
import { exportLabelPreviewToPng } from "../utils/exportLabelPreview";

const MOCK_LABEL = {
  materialName: "BOLSA LAMINADA ALTA BARRERA 25KG",
  codigo: "MAT-ACD-001245",
  lote: "250312-MEM0003454",
  fecha: "12/03/2026",
  caducidad: "12/09/2026",
  reanalisis: "12/06/2026",
  cantidad: "40 SACOS",
  envaseNum: "01",
  envaseTotal: "40",
  publicToken: "a4c93f31-7c13-4d8d-95e5-f1a1e8b2caa1",
};

export default function LabelPreviewTestPage() {
  const previewWrapRef = useRef<HTMLDivElement>(null);
  const [qrDataUrl, setQrDataUrl] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    const payload = `OLNQR:1:${MOCK_LABEL.publicToken}`;
    const logoPath = `${import.meta.env.BASE_URL}logo-olnatura.png`;
    generateQrWithLogo(payload, {
      errorCorrectionLevel: "H",
      margin: 2,
      width: 640,
      logoUrl: logoPath,
      logoSizeRatio: 0.22,
    })
      .then(setQrDataUrl)
      .catch(() => setQrDataUrl(null));
  }, []);

  const onDownloadPng = async () => {
    const el = previewWrapRef.current?.querySelector("[data-label-preview]") as HTMLElement | null;
    if (!el) return;
    try {
      setBusy(true);
      const dataUrl = await exportLabelPreviewToPng(el);
      const a = document.createElement("a");
      a.href = dataUrl;
      a.download = `LABEL_PREVIEW_TEST_${MOCK_LABEL.lote}.png`;
      document.body.appendChild(a);
      a.click();
      a.remove();
    } finally {
      setBusy(false);
    }
  };

  return (
    <div style={{ display: "grid", gap: 14 }}>
      <div>
        <Text weight="semibold" size={700}>
          Label Preview Playground
        </Text>
        <div style={{ color: "#6B6B6B", marginTop: 4 }}>
          Sandbox aislado para perfeccionar el layout visual de la etiqueta de almacén.
        </div>
      </div>

      <Card style={{ padding: 16, display: "grid", gap: 12, maxWidth: 840 }}>
        <div style={{ display: "flex", gap: 10, flexWrap: "wrap" }}>
          <Button appearance="secondary" onClick={onDownloadPng} disabled={busy}>
            {busy ? "Generando PNG..." : "Descargar PNG (Playground)"}
          </Button>
        </div>

        <div
          ref={previewWrapRef}
          style={{
            width: "100%",
            overflowX: "auto",
            paddingTop: 16,
          }}
        >
          <div
            style={{
              width: "fit-content",
              margin: "0 auto",
            }}
          >
            <div
              style={{
                width: 100,
                height: 90,
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
              materialName={MOCK_LABEL.materialName}
              codigo={MOCK_LABEL.codigo}
              lote={MOCK_LABEL.lote}
              fecha={MOCK_LABEL.fecha}
              caducidad={MOCK_LABEL.caducidad}
              reanalisis={MOCK_LABEL.reanalisis}
              cantidad={MOCK_LABEL.cantidad}
              envaseNum={MOCK_LABEL.envaseNum}
              envaseTotal={MOCK_LABEL.envaseTotal}
              qrData={qrDataUrl}
            />
              </div>
            </div>
          </div>
        </div>
      </Card>
    </div>
  );
}
