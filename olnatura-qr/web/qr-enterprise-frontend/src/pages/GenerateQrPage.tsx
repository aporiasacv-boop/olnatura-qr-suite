import { useMemo, useState } from "react";
import { Button, Card, Input, Text, makeStyles, shorthands } from "@fluentui/react-components";
import { generateQrWithLogo } from "../utils/qrWithLogo";

const useStyles = makeStyles({
  wrap: { display: "grid", gap: "14px", maxWidth: "560px" },
  row: { display: "grid", gap: "6px" },
  preview: {
    display: "grid",
    placeItems: "center",
    backgroundColor: "#fff",
    borderRadius: "12px",
    ...shorthands.padding("16px"),
    boxShadow: "0 1px 2px rgba(0,0,0,0.08)",
  },
  img: { width: "320px", height: "320px", objectFit: "contain" },
  actions: { display: "flex", gap: "10px", flexWrap: "wrap" },
});

export default function GenerateQrPage() {
  const s = useStyles();

  const [lote, setLote] = useState("");
  const [pngDataUrl, setPngDataUrl] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fileName = useMemo(() => {
    const safe = (lote || "QR").trim().replace(/[^\w\-]+/g, "_");
    return `QR_${safe}.png`;
  }, [lote]);

  async function generate() {
    setBusy(true);
    setError(null);
    setPngDataUrl(null);

    const v = (lote || "").trim();
    if (!v) {
      setBusy(false);
      setError("Escribe un lote.");
      return;
    }

    try {
      const payload = v;

      // Respetar BASE_URL (por si algún día se publica bajo subruta)
      const logoPath = `${import.meta.env.BASE_URL}logo-olnatura.png`;

      const dataUrl = await generateQrWithLogo(payload, {
        errorCorrectionLevel: "H",
        margin: 4,
        width: 800,
        logoUrl: logoPath,
        logoSizeRatio: 0.22,
        debug: true,
      });

      setPngDataUrl(dataUrl);
    } catch (e: unknown) {
      const msg = e instanceof Error ? e.message : "No se pudo generar el QR.";
      setError(msg);
    } finally {
      setBusy(false);
    }
  }

  function download() {
    if (!pngDataUrl) return;
    const a = document.createElement("a");
    a.href = pngDataUrl;
    a.download = fileName;
    document.body.appendChild(a);
    a.click();
    a.remove();
  }

  return (
    <div className={s.wrap}>
      <Text size={600} weight="semibold">
        Generar QR (PNG)
      </Text>
      <Text size={300} style={{ opacity: 0.75 }}>
        Solo ADMIN/ALMACÉN. El QR contiene el LOTE. Logo centrado, nivel H para buena escaneabilidad.
      </Text>

      <Card>
        <div style={{ padding: 16, display: "grid", gap: 12 }}>
          <div className={s.row}>
            <Text>Lote</Text>
            <Input
              value={lote}
              onChange={(_, d) => setLote(d.value)}
              placeholder="Ej: LOTE-TEST-001"
            />
          </div>

          <div className={s.actions}>
            <Button appearance="primary" onClick={generate} disabled={busy}>
              {busy ? "Generando…" : "Generar"}
            </Button>

            <Button onClick={download} disabled={!pngDataUrl}>
              Descargar PNG
            </Button>
          </div>

          {error ? <Text style={{ color: "#B10E1C" }}>{error}</Text> : null}

          <div className={s.preview}>
            {pngDataUrl ? (
              <img src={pngDataUrl} alt="QR preview" className={s.img} />
            ) : (
              <Text style={{ opacity: 0.6 }}>Vista previa aquí</Text>
            )}
          </div>
        </div>
      </Card>
    </div>
  );
}
