import { useMemo, useState } from "react";
import { Card, Text, Input, Button } from "@fluentui/react-components";
import QRCode from "qrcode";
import { useAuth } from "../auth/AuthContext";
import { generateQrWithLogo } from "../utils/qrWithLogo";

type FormState = {
  tipoMaterial: string;
  nombre: string;
  codigo: string;
  lote: string;
  fechaEntrada: string;
  caducidad: string;
  envaseNum: string;
  envaseTotal: string;
};

export default function RegisterLabelPage() {
  const { me } = useAuth();

  const canQr = useMemo(() => {
    const roles = me?.roles ?? [];
    return roles.includes("ADMIN") || roles.includes("ALMACEN");
  }, [me]);

  const [form, setForm] = useState<FormState>({
    tipoMaterial: "",
    nombre: "",
    codigo: "",
    lote: "",
    fechaEntrada: "",
    caducidad: "",
    envaseNum: "",
    envaseTotal: "",
  });

  const [busy, setBusy] = useState(false);
  const [qrDataUrl, setQrDataUrl] = useState<string | null>(null);
  const [err, setErr] = useState<string | null>(null);

  const loteOk = form.lote.trim().length > 0;
  const canGenerate = canQr && loteOk && !busy;

  const payload = useMemo(() => {
    // Esto es lo que “vive” dentro del QR. Lo puedes ajustar al contrato final.
    // Por ahora: incluye lote + algunos campos útiles.
    return JSON.stringify(
      {
        lote: form.lote.trim(),
        codigo: form.codigo.trim() || null,
        nombre: form.nombre.trim() || null,
        envaseNum: form.envaseNum.trim() || null,
        envaseTotal: form.envaseTotal.trim() || null,
        ts: new Date().toISOString(),
      },
      null,
      0
    );
  }, [form]);

  const onGenerateQr = async () => {
    setErr(null);

    if (!canQr) {
      setErr("No autorizado. Solo ADMIN y ALMACÉN pueden generar QR.");
      return;
    }
    if (!loteOk) {
      setErr("Captura un lote para generar el QR.");
      return;
    }

    try {
      setBusy(true);

      // ✅ Mantén el mismo payload, pero genera con logo centrado
      const logoPath = `${import.meta.env.BASE_URL}logo-olnatura.png`;

      const dataUrl = await generateQrWithLogo(payload, {
        errorCorrectionLevel: "H",
        margin: 2,
        width: 640,
        logoUrl: logoPath,
        logoSizeRatio: 0.22,
        debug: true,
      });

      setQrDataUrl(dataUrl);
    } catch (e) {
      setErr("No se pudo generar el QR.");
      setQrDataUrl(null);
    } finally {
      setBusy(false);
    }
  };

  const onDownloadPng = () => {
    setErr(null);
    if (!qrDataUrl) {
      setErr("Primero genera el QR.");
      return;
    }

    const lote = form.lote.trim() || "qr";
    const a = document.createElement("a");
    a.href = qrDataUrl;
    a.download = `QR_${lote}.png`;
    document.body.appendChild(a);
    a.click();
    a.remove();
  };

  return (
    <div style={{ display: "grid", gap: 14 }}>
      <div>
        <Text weight="semibold" size={700}>
          Registrar etiqueta
        </Text>

        <div style={{ color: "#6B6B6B", marginTop: 4 }}>
          UI lista. Generación de QR es local (frontend). Endpoint de registro aún pendiente.
        </div>

        {!canQr ? (
          <div style={{ color: "#8A6D00", marginTop: 8 }}>
            Tu rol no permite generar/descargar QR (solo ADMIN y ALMACÉN).
          </div>
        ) : null}

        {err ? (
          <div style={{ color: "#B10E1C", marginTop: 8 }}>{err}</div>
        ) : null}
      </div>

      <Card style={{ padding: 16, display: "grid", gap: 12, maxWidth: 720 }}>
        <Field
          label="Tipo material"
          placeholder="Ej. MP"
          value={form.tipoMaterial}
          onChange={(v) => setForm((s) => ({ ...s, tipoMaterial: v }))}
        />
        <Field
          label="Nombre"
          placeholder="Nombre de material"
          value={form.nombre}
          onChange={(v) => setForm((s) => ({ ...s, nombre: v }))}
        />
        <Field
          label="Código"
          placeholder="Código interno"
          value={form.codigo}
          onChange={(v) => setForm((s) => ({ ...s, codigo: v }))}
        />
        <Field
          label="Lote"
          placeholder="LOT-..."
          value={form.lote}
          onChange={(v) => setForm((s) => ({ ...s, lote: v }))}
        />
        <Field
          label="Fecha entrada"
          placeholder="YYYY-MM-DD"
          value={form.fechaEntrada}
          onChange={(v) => setForm((s) => ({ ...s, fechaEntrada: v }))}
        />
        <Field
          label="Caducidad/Reanálisis"
          placeholder="YYYY-MM-DD"
          value={form.caducidad}
          onChange={(v) => setForm((s) => ({ ...s, caducidad: v }))}
        />
        <Field
          label="Envase Num"
          placeholder="1"
          value={form.envaseNum}
          onChange={(v) => setForm((s) => ({ ...s, envaseNum: v }))}
        />
        <Field
          label="Envase Total"
          placeholder="20"
          value={form.envaseTotal}
          onChange={(v) => setForm((s) => ({ ...s, envaseTotal: v }))}
        />

        <div style={{ display: "flex", gap: 10, flexWrap: "wrap" }}>
          <Button appearance="primary" onClick={onGenerateQr} disabled={!canGenerate}>
            {busy ? "Generando…" : "Generar QR"}
          </Button>

          <Button
            appearance="secondary"
            onClick={onDownloadPng}
            disabled={!canQr || !qrDataUrl || busy}
          >
            Descargar PNG
          </Button>
        </div>

        {qrDataUrl ? (
          <div style={{ display: "grid", gap: 8, marginTop: 6 }}>
            <Text weight="semibold">Preview</Text>
            <div
              style={{
                display: "grid",
                placeItems: "center",
                background: "#fff",
                borderRadius: 12,
                padding: 12,
                border: "1px solid rgba(0,0,0,0.08)",
              }}
            >
              <img
                src={qrDataUrl}
                alt="QR preview"
                style={{ width: 320, height: 320, objectFit: "contain" }}
              />
            </div>
            <div style={{ color: "#6B6B6B", fontSize: 12 }}>
              Contenido codificado: lote + metadata básica (modo demo). Luego lo alineamos al payload final.
            </div>
          </div>
        ) : null}
      </Card>
    </div>
  );
}

function Field({
  label,
  placeholder,
  value,
  onChange,
}: {
  label: string;
  placeholder: string;
  value: string;
  onChange: (v: string) => void;
}) {
  return (
    <div style={{ display: "grid", gap: 6 }}>
      <Text>{label}</Text>
      <Input value={value} onChange={(_, d) => onChange(d.value)} placeholder={placeholder} />
    </div>
  );
}