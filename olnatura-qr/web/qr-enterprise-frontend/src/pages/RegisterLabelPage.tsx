import { useMemo, useState } from "react";
import { Card, Text, Input, Button, Radio, RadioGroup } from "@fluentui/react-components";
import { useAuth } from "../auth/AuthContext";
import { api, ApiError } from "../api/client";
import { generateQrWithLogo } from "../utils/qrWithLogo";
import { renderLabelToPng, type FechaTipo } from "../utils/labelToPng";

const QR_PREFIX = "OLNQR:1:";

type FormState = {
  tipoMaterial: string;
  nombre: string;
  codigo: string;
  lote: string;
  fechaEntrada: string;
  fechaTipo: FechaTipo;
  fechaValor: string;
  envaseNum: string;
  envaseTotal: string;
};

type CreateResponse = {
  id: string;
  status: string;
  qrUrl: string;
  publicToken: string;
  label: Record<string, any>;
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
    fechaTipo: "CADUCIDAD",
    fechaValor: "",
    envaseNum: "",
    envaseTotal: "",
  });

  const [busy, setBusy] = useState(false);
  const [qrDataUrl, setQrDataUrl] = useState<string | null>(null);
  const [createResp, setCreateResp] = useState<CreateResponse | null>(null);
  const [err, setErr] = useState<string | null>(null);

  const loteOk = form.lote.trim().length > 0;
  const fechaEntradaOk = /^\d{4}-\d{2}-\d{2}$/.test(form.fechaEntrada.trim());
  const envaseNum = parseInt(form.envaseNum, 10) || 0;
  const envaseTotal = parseInt(form.envaseTotal, 10) || 0;
  const envaseOk = envaseNum > 0 && envaseTotal > 0 && envaseNum <= envaseTotal;
  const canRegister = canQr && loteOk && fechaEntradaOk && envaseOk && !busy;

  const onRegisterAndGenerate = async () => {
    setErr(null);
    setCreateResp(null);
    setQrDataUrl(null);

    if (!canQr) {
      setErr("No autorizado. Solo ADMIN y ALMACÉN pueden registrar.");
      return;
    }
    if (!loteOk) {
      setErr("Captura un lote.");
      return;
    }
    if (!fechaEntradaOk) {
      setErr("Fecha de entrada requerida (YYYY-MM-DD).");
      return;
    }
    if (!envaseOk) {
      setErr("Envase Num/Total deben ser > 0 y envaseNum ≤ envaseTotal.");
      return;
    }

    try {
      setBusy(true);
      const caducidad = form.fechaTipo === "CADUCIDAD" && form.fechaValor.trim() ? form.fechaValor.trim() : null;
      const reanalisis = form.fechaTipo === "REANALISIS" && form.fechaValor.trim() ? form.fechaValor.trim() : null;
      const body = {
        tipoMaterial: form.tipoMaterial.trim() || "MP",
        nombre: form.nombre.trim() || form.lote.trim(),
        codigo: form.codigo.trim() || form.lote.trim(),
        lote: form.lote.trim(),
        fechaEntrada: form.fechaEntrada.trim(),
        caducidad,
        reanalisis,
        envaseNum,
        envaseTotal,
      };
      const res = await api<CreateResponse>("/label", { method: "POST", body });
      setCreateResp(res);
      const payload = QR_PREFIX + res.publicToken;
      const logoPath = `${import.meta.env.BASE_URL}logo-olnatura.png`;
      const dataUrl = await generateQrWithLogo(payload, {
        errorCorrectionLevel: "H",
        margin: 2,
        width: 640,
        logoUrl: logoPath,
        logoSizeRatio: 0.22,
      });
      setQrDataUrl(dataUrl);
    } catch (e) {
      const ae = e as ApiError;
      setErr(ae?.message ?? "No se pudo registrar la etiqueta.");
      setQrDataUrl(null);
      setCreateResp(null);
    } finally {
      setBusy(false);
    }
  };

  const onDownloadPng = async () => {
    setErr(null);
    if (!qrDataUrl) {
      setErr("Primero registra y genera el QR.");
      return;
    }
    try {
      setBusy(true);
      const labelPng = await renderLabelToPng(
        {
          tipoMaterial: form.tipoMaterial.trim(),
          nombre: form.nombre.trim(),
          codigo: form.codigo.trim(),
          lote: form.lote.trim(),
          fechaEntrada: form.fechaEntrada.trim(),
          fechaTipo: form.fechaTipo,
          fechaValor: form.fechaValor.trim(),
          envaseNum: form.envaseNum ? Number(form.envaseNum) : undefined,
          envaseTotal: form.envaseTotal ? Number(form.envaseTotal) : undefined,
        },
        qrDataUrl
      );
      const lote = form.lote.trim() || "etiqueta";
      const a = document.createElement("a");
      a.href = labelPng;
      a.download = `ETIQUETA_${lote}.png`;
      document.body.appendChild(a);
      a.click();
      a.remove();
    } catch (e) {
      setErr("No se pudo generar la etiqueta imprimible.");
    } finally {
      setBusy(false);
    }
  };

  return (
    <div style={{ display: "grid", gap: 14 }}>
      <div>
        <Text weight="semibold" size={700}>
          Registrar etiqueta
        </Text>

        <div style={{ color: "#6B6B6B", marginTop: 4 }}>
          Registra la etiqueta en el sistema, genera QR con token y etiqueta imprimible.
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
        <div style={{ display: "grid", gap: 6 }}>
          <Text>Fecha (tipo)</Text>
          <RadioGroup
            value={form.fechaTipo}
            onChange={(_, d) => setForm((s) => ({ ...s, fechaTipo: d.value as FechaTipo }))}
            layout="horizontal"
          >
            <Radio value="CADUCIDAD" label="Caducidad" />
            <Radio value="REANALISIS" label="Reanálisis" />
          </RadioGroup>
        </div>
        <Field
          label={form.fechaTipo === "CADUCIDAD" ? "Caducidad (YYYY-MM-DD)" : "Reanálisis (YYYY-MM-DD)"}
          placeholder="YYYY-MM-DD"
          value={form.fechaValor}
          onChange={(v) => setForm((s) => ({ ...s, fechaValor: v }))}
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
          <Button appearance="primary" onClick={onRegisterAndGenerate} disabled={!canRegister}>
            {busy ? "Registrando…" : "Registrar y generar QR"}
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
            <Text weight="semibold">Vista previa</Text>
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
              Formato: OLNQR:1:&lt;token&gt;. La etiqueta queda persistida en el sistema.
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