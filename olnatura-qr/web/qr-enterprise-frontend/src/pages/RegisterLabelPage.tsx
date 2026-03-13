import { useMemo, useRef, useState } from "react";
import { Card, Text, Input, Button, Radio, RadioGroup } from "@fluentui/react-components";
import { useAuth } from "../auth/AuthContext";
import { api, ApiError, API_BASE } from "../api/client";
import { generateQrPlain } from "../utils/qrWithLogo";
import { parseDDMMYYYYToISO, isValidDDMMYYYY } from "../utils/dateFormat";
import { exportLabelPreviewToPng } from "../utils/exportLabelPreview";
import LabelPreview from "../components/label/LabelPreview";
import type { FechaTipo } from "../utils/labelToPng";

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
  const previewRef = useRef<HTMLDivElement>(null);

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
  const fechaEntradaOk = isValidDDMMYYYY(form.fechaEntrada);
  const envaseNum = parseInt(form.envaseNum, 10) || 0;
  const envaseTotal = parseInt(form.envaseTotal, 10) || 0;
  const envaseOk = envaseNum > 0 && envaseTotal > 0 && envaseNum <= envaseTotal;
  const canRegister = canQr && loteOk && fechaEntradaOk && envaseOk && !busy;

  const caducidadDisplay = form.fechaTipo === "CADUCIDAD" ? form.fechaValor : "";
  const reanalisisDisplay = form.fechaTipo === "REANALISIS" ? form.fechaValor : "";

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
      setErr("Fecha de entrada requerida (DD/MM/YYYY).");
      return;
    }
    if (!envaseOk) {
      setErr("Envase Num/Total deben ser > 0 y Envase Num ≤ Cantidad total.");
      return;
    }

    const fechaEntradaIso = parseDDMMYYYYToISO(form.fechaEntrada);
    if (!fechaEntradaIso) {
      setErr("Formato de fecha inválido. Usa DD/MM/YYYY.");
      return;
    }

    const fechaValorIso =
      form.fechaValor.trim() && isValidDDMMYYYY(form.fechaValor)
        ? parseDDMMYYYYToISO(form.fechaValor)
        : null;

    try {
      setBusy(true);
      const caducidad =
        form.fechaTipo === "CADUCIDAD" && fechaValorIso ? fechaValorIso : null;
      const reanalisis =
        form.fechaTipo === "REANALISIS" && fechaValorIso ? fechaValorIso : null;
      const body = {
        tipoMaterial: form.tipoMaterial.trim() || "MP",
        nombre: form.nombre.trim() || form.lote.trim(),
        codigo: form.codigo.trim() || form.lote.trim(),
        lote: form.lote.trim(),
        fechaEntrada: fechaEntradaIso,
        caducidad,
        reanalisis,
        envaseNum,
        envaseTotal,
      };
      const res = await api<CreateResponse>("/label", { method: "POST", body });
      setCreateResp(res);
      const payload = QR_PREFIX + res.publicToken;
      const dataUrl = await generateQrPlain(payload, { width: 220, margin: 2 });
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

  const onScrollToPreview = () => {
    previewRef.current?.scrollIntoView({ behavior: "smooth", block: "center" });
  };

  const onDownloadPng = async () => {
    setErr(null);
    const el = previewRef.current?.querySelector("[data-label-preview]") as HTMLElement;
    if (!el) {
      setErr("Vista previa no disponible. Registra primero la etiqueta.");
      return;
    }
    try {
      setBusy(true);
      const dataUrl = await exportLabelPreviewToPng(el);
      const lote = form.lote.trim() || "etiqueta";
      const a = document.createElement("a");
      a.href = dataUrl;
      a.download = `ETIQUETA_${lote}.png`;
      document.body.appendChild(a);
      a.click();
      a.remove();
    } catch (e) {
      setErr("No se pudo generar el PNG.");
    } finally {
      setBusy(false);
    }
  };

  const onDownloadZpl = async () => {
    setErr(null);
    if (!createResp?.id) {
      setErr("Primero registra la etiqueta para descargar ZPL.");
      return;
    }
    try {
      setBusy(true);
      const total = envaseTotal || 1;
      const base = API_BASE || "";
      const url = `${base}/api/v1/label/${createResp.id}/zpl`;
      const res = await fetch(url, {
        method: "POST",
        credentials: "include",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          total,
          from: 1,
          to: total,
          qrImageBase64: qrDataUrl || undefined,
        }),
      });
      if (!res.ok) throw new Error("No se pudo descargar el archivo ZPL.");
      const blob = await res.blob();
      const lote = form.lote.trim().replace(/[^\w\-]+/g, "_");
      const filename = `etiqueta-${lote}.zpl`;
      const a = document.createElement("a");
      a.href = URL.createObjectURL(blob);
      a.download = filename;
      document.body.appendChild(a);
      a.click();
      a.remove();
      URL.revokeObjectURL(a.href);
    } catch (e) {
      setErr("No se pudo descargar la etiqueta Zebra (.zpl).");
    } finally {
      setBusy(false);
    }
  };

  const hasPreview = !!qrDataUrl;

  return (
    <div style={{ display: "grid", gap: 14 }}>
      <div>
        <Text weight="semibold" size={700}>
          Registrar etiqueta
        </Text>

        <div style={{ color: "#6B6B6B", marginTop: 4 }}>
          Registra la etiqueta en el sistema, genera QR con token y descarga PNG o etiqueta Zebra.
        </div>

        {!canQr ? (
          <div style={{ color: "#8A6D00", marginTop: 8 }}>
            Tu rol no permite generar/descargar etiquetas (solo ADMIN y ALMACÉN).
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
          label="Fecha entrada (DD/MM/YYYY)"
          placeholder="11/09/2025"
          value={form.fechaEntrada}
          onChange={(v) => setForm((s) => ({ ...s, fechaEntrada: v }))}
        />
        <div style={{ display: "grid", gap: 6 }}>
          <Text>Fecha (tipo)</Text>
          <RadioGroup
            value={form.fechaTipo}
            onChange={(_, d) =>
              setForm((s) => ({ ...s, fechaTipo: d.value as FechaTipo }))
            }
            layout="horizontal"
          >
            <Radio value="CADUCIDAD" label="Caducidad" />
            <Radio value="REANALISIS" label="Reanálisis" />
          </RadioGroup>
        </div>
        <Field
          label={
            form.fechaTipo === "CADUCIDAD"
              ? "Caducidad (DD/MM/YYYY)"
              : "Reanálisis (DD/MM/YYYY)"
          }
          placeholder="11/09/2025"
          value={form.fechaValor}
          onChange={(v) => setForm((s) => ({ ...s, fechaValor: v }))}
        />
        <Field
          label="Envase No"
          placeholder="1"
          value={form.envaseNum}
          onChange={(v) => setForm((s) => ({ ...s, envaseNum: v }))}
          hint="Contenedor actual (ej: 1 de 40)"
        />
        <Field
          label="Cantidad total"
          placeholder="40"
          value={form.envaseTotal}
          onChange={(v) => setForm((s) => ({ ...s, envaseTotal: v }))}
          hint="Total de contenedores"
        />

        <div style={{ display: "flex", gap: 10, flexWrap: "wrap" }}>
          <Button
            appearance="primary"
            onClick={onRegisterAndGenerate}
            disabled={!canRegister}
          >
            {busy ? "Registrando…" : "Registrar y generar QR"}
          </Button>
        </div>

        <div
          ref={previewRef}
          style={{ display: "grid", gap: 12, marginTop: 8 }}
        >
          <Text weight="semibold">Vista previa de la etiqueta</Text>

          <div
            style={{
              display: "grid",
              placeItems: "start",
              background: "#f9fafb",
              borderRadius: 12,
              padding: 16,
              border: "1px solid rgba(0,0,0,0.08)",
              overflowX: "auto",
            }}
          >
            <div
              style={{
                width: 400,
                height: 300,
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
                  materialName={form.nombre.trim() || form.lote.trim() || "—"}
                  codigo={form.codigo.trim() || "—"}
                  lote={form.lote.trim() || "—"}
                  fecha={form.fechaEntrada}
                  caducidad={caducidadDisplay}
                  reanalisis={reanalisisDisplay}
                  cantidad="N/A"
                  envaseNum={form.envaseNum || "—"}
                  envaseTotal={form.envaseTotal || "—"}
                  qrData={qrDataUrl}
                  logoUrl={`${import.meta.env.BASE_URL}logo-olnatura.png`}
                  documentCode={createResp?.label?.documentCode ?? "AL-001-E02/04"}
                />
              </div>
            </div>
          </div>

          <div style={{ display: "flex", gap: 10, flexWrap: "wrap" }}>
            <Button
              appearance="secondary"
              onClick={onScrollToPreview}
            >
              Vista previa
            </Button>
            <Button
              appearance="secondary"
              onClick={onDownloadPng}
              disabled={!canQr || !hasPreview || busy}
            >
              Descargar PNG
            </Button>
            <Button
              appearance="secondary"
              onClick={onDownloadZpl}
              disabled={!canQr || !createResp || busy}
            >
              Descargar etiqueta Zebra (.zpl)
            </Button>
          </div>

          {qrDataUrl ? (
            <div style={{ color: "#6B6B6B", fontSize: 12 }}>
              Formato QR: OLNQR:1:&lt;token&gt;. La etiqueta queda persistida en el sistema.
            </div>
          ) : null}
        </div>
      </Card>
    </div>
  );
}

function Field({
  label,
  placeholder,
  value,
  onChange,
  hint,
}: {
  label: string;
  placeholder: string;
  value: string;
  onChange: (v: string) => void;
  hint?: string;
}) {
  return (
    <div style={{ display: "grid", gap: 6 }}>
      <Text>{label}</Text>
      <Input
        value={value}
        onChange={(_, d) => onChange(d.value)}
        placeholder={placeholder}
      />
      {hint ? (
        <Text style={{ fontSize: 12, color: "#6B7280" }}>{hint}</Text>
      ) : null}
    </div>
  );
}
