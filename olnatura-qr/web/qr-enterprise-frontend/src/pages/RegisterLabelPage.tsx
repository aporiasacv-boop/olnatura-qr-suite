import { useMemo, useState } from "react";
import { Card, Text, Input, Button } from "@fluentui/react-components";
import QRCode from "qrcode";
import { useAuth } from "../auth/AuthContext";

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

  const payload = form.lote.trim();

  const labelTitle = useMemo(() => {
    const t = form.tipoMaterial.trim();
    const n = form.nombre.trim();
    const c = form.codigo.trim();
    const l = form.lote.trim();
    const hasAny = t || n || c || l;
    if (!hasAny) return "Etiqueta QR de Lote";
    return "Etiqueta QR de Lote";
  }, [form]);

  const safeText = (v: string) => v.trim() || "—";

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

      // QR limpio, listo para impresión. Logo se sobrepone con “sello”.
      const dataUrl = await QRCode.toDataURL(payload, {
        errorCorrectionLevel: "H", // más tolerante con logo encima
        margin: 2,
        width: 900, // nítido al descargar/impresión
        color: {
          dark: "#1C1C1C",
          light: "#FFFFFF",
        },
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

  const QrPreview = () => {
    if (!qrDataUrl) return null;

    return (
      <div style={{ display: "grid", gap: 10, marginTop: 6 }}>
        <div style={{ display: "flex", alignItems: "baseline", justifyContent: "space-between", gap: 10 }}>
          <Text weight="semibold">Preview</Text>
          <Text size={200} style={{ color: "#6B6B6B" }}>
            Listo para imprimir (PNG)
          </Text>
        </div>

        {/* “Etiqueta” completa */}
        <div
          style={{
            borderRadius: 20,
            padding: 6,
            background: "linear-gradient(135deg, #0B6A3B, #1FA67A)",
            boxShadow: "0 12px 30px rgba(0,0,0,0.15)",
            maxWidth: 520,
          }}
        >
          {/* Área blanca */}
          <div
            style={{
              background: "#FFFFFF",
              borderRadius: 16,
              padding: 16,
              display: "grid",
              gap: 12,
            }}
          >
            {/* Encabezado */}
            <div style={{ display: "grid", gap: 6 }}>
              <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: 10 }}>
                <div style={{ display: "grid", gap: 2 }}>
                  <Text
                    weight="semibold"
                    size={600}
                    style={{
                      letterSpacing: 0.3,
                      lineHeight: 1.1,
                    }}
                  >
                    OLNATURA
                  </Text>
                  <Text size={200} style={{ color: "#6B6B6B" }}>
                    {labelTitle}
                  </Text>
                </div>

                {/* Sello “status” visual (solo estético por ahora) */}
                <div
                  style={{
                    fontSize: 12,
                    padding: "6px 10px",
                    borderRadius: 999,
                    background: "rgba(11,106,59,0.10)",
                    border: "1px solid rgba(11,106,59,0.25)",
                    color: "#0B6A3B",
                    fontWeight: 600,
                    whiteSpace: "nowrap",
                  }}
                  title="Indicador visual (aún no dinámico)"
                >
                  QR LOTE
                </div>
              </div>

              <div
                style={{
                  height: 1,
                  background: "rgba(0,0,0,0.08)",
                  marginTop: 6,
                }}
              />
            </div>

            {/* QR + logo al centro */}
            <div
              style={{
                display: "grid",
                placeItems: "center",
                background: "#fff",
                borderRadius: 14,
                padding: 14,
                border: "1px solid rgba(0,0,0,0.08)",
              }}
            >
              <div style={{ position: "relative", width: 340, height: 340 }}>
                <img
                  src={qrDataUrl}
                  alt="QR"
                  style={{
                    width: 340,
                    height: 340,
                    objectFit: "contain",
                    display: "block",
                    borderRadius: 12,
                  }}
                />

                {/* Logo centrado (sello elegante) */}
                <div
                  style={{
                    position: "absolute",
                    inset: 0,
                    display: "grid",
                    placeItems: "center",
                    pointerEvents: "none",
                  }}
                >
                  <div
                    style={{
                      width: 78,
                      height: 78,
                      borderRadius: "50%",
                      background: "#FFFFFF",
                      padding: 9,
                      boxShadow: "0 10px 24px rgba(0,0,0,0.25)",
                      border: "1px solid rgba(0,0,0,0.08)",
                      display: "grid",
                      placeItems: "center",
                    }}
                    title="logo"
                  >
                    <img
                      src="/logo-olnatura.png"
                      alt="Logo Olnatura"
                      style={{
                        width: "100%",
                        height: "100%",
                        objectFit: "contain",
                        display: "block",
                        filter: "drop-shadow(0 1px 1px rgba(0,0,0,0.10))",
                      }}
                      onError={(ev) => {
                        // Si el asset no carga, no rompemos nada: simplemente ocultamos el img
                        (ev.currentTarget as HTMLImageElement).style.display = "none";
                      }}
                    />
                  </div>
                </div>
              </div>
            </div>

            {/* Datos estáticos (lo que se imprime) */}
            <div
              style={{
                display: "grid",
                gap: 6,
                paddingTop: 4,
              }}
            >
              <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 10 }}>
                <InfoRow label="Tipo" value={safeText(form.tipoMaterial)} />
                <InfoRow label="Nombre" value={safeText(form.nombre)} />
                <InfoRow label="Código" value={safeText(form.codigo)} />
                <InfoRow label="Lote" value={safeText(form.lote)} />
                <InfoRow label="Envase" value={`${safeText(form.envaseNum)} / ${safeText(form.envaseTotal)}`} />
                <InfoRow label="Entrada" value={safeText(form.fechaEntrada)} />
                <InfoRow label="Caducidad/Reanálisis" value={safeText(form.caducidad)} />
              </div>

              <div style={{ color: "#6B6B6B", fontSize: 12, marginTop: 2 }}>
                Contenido codificado: lote + metadata básica (modo demo).
              </div>
            </div>
          </div>
        </div>
      </div>
    );
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

        {err ? <div style={{ color: "#B10E1C", marginTop: 8 }}>{err}</div> : null}
      </div>

      <Card style={{ padding: 16, display: "grid", gap: 12, maxWidth: 820 }}>
        <div
          style={{
            display: "grid",
            gridTemplateColumns: "1fr 1fr",
            gap: 12,
          }}
        >
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
        </div>

        <div style={{ display: "flex", gap: 10, flexWrap: "wrap" }}>
          <Button appearance="primary" onClick={onGenerateQr} disabled={!canGenerate}>
            {busy ? "Generando…" : "Generar QR"}
          </Button>

          <Button appearance="secondary" onClick={onDownloadPng} disabled={!canQr || !qrDataUrl || busy}>
            Descargar PNG
          </Button>
        </div>

        <QrPreview />
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

function InfoRow({ label, value }: { label: string; value: string }) {
  return (
    <div
      style={{
        display: "grid",
        gridTemplateColumns: "120px 1fr",
        gap: 8,
        alignItems: "baseline",
        padding: "6px 10px",
        borderRadius: 12,
        background: "rgba(0,0,0,0.03)",
        border: "1px solid rgba(0,0,0,0.06)",
      }}
    >
      <div style={{ color: "#4B4B4B", fontSize: 12, fontWeight: 700, letterSpacing: 0.2 }}>
        {label}:
      </div>
      <div style={{ color: "#111", fontSize: 12, fontWeight: 600, wordBreak: "break-word" }}>{value}</div>
    </div>
  );
}