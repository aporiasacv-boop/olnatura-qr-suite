import { useMemo, useRef, useState } from "react";
import { Text, Input, Button, Radio, RadioGroup } from "@fluentui/react-components";
import AppCard from "../components/ui/AppCard";
import { brand } from "../styles/brand";
import { useAuth } from "../auth/AuthContext";
import { api, ApiError } from "../api/client";
import { downloadLabelZplFile } from "../utils/downloadLabelZpl";
import { generateQrPlain } from "../utils/qrWithLogo";
import { isValidDDMMYYYY } from "../utils/dateFormat";
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
  cantidadPorEnvase: string;
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
    cantidadPorEnvase: "",
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
      setErr("No autorizado.");
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
    if (form.fechaTipo === "CADUCIDAD" && form.fechaValor.trim() && !isValidDDMMYYYY(form.fechaValor)) {
      setErr("Caducidad: revisa la fecha (puedes usar p. ej. 10/6/26 o 10/06/2026).");
      return;
    }
    if (form.fechaTipo === "REANALISIS" && form.fechaValor.trim() && !isValidDDMMYYYY(form.fechaValor)) {
      setErr("Reanálisis: revisa la fecha.");
      return;
    }

    try {
      setBusy(true);
      const fv = form.fechaValor.trim();
      const caducidad =
        form.fechaTipo === "CADUCIDAD" && fv.length > 0 ? fv : null;
      const reanalisis =
        form.fechaTipo === "REANALISIS" && fv.length > 0 ? fv : null;
      const cpe = form.cantidadPorEnvase.trim();
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
        cantidadPorEnvase: cpe.length > 0 ? cpe : null,
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
      await downloadLabelZplFile({
        labelIdOrLote: String(createResp.id),
        totalEnvases: total,
        printFrom: 1,
        printTo: total,
      });
    } catch {
      setErr("No se pudo descargar la etiqueta Zebra (.zpl). Comprueba la sesión y vuelve a intentar.");
    } finally {
      setBusy(false);
    }
  };

  const hasPreview = !!qrDataUrl;

  return (
    <div style={{ display: "grid", gap: 24 }}>
      <div>
        <h1 style={{ fontSize: "20px", fontWeight: 600, color: brand.text, margin: 0 }}>Registrar etiqueta</h1>
        {!canQr ? (
          <div style={{ color: brand.warningFg, marginTop: 8, fontSize: 14 }}>No tienes permisos para esta acción.</div>
        ) : null}
        {err ? <div style={{ color: brand.dangerFg, marginTop: 8, fontSize: 14 }}>{err}</div> : null}
      </div>

      <AppCard style={{ display: "grid", gap: 16, maxWidth: 720 }}>
        <Text style={{ fontSize: 15, fontWeight: 600, color: brand.text }}>Datos de la etiqueta</Text>
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
        <Field
          label="Cantidad por envase"
          placeholder="Ej. 25 kg o 1000"
          value={form.cantidadPorEnvase}
          onChange={(v) => setForm((s) => ({ ...s, cantidadPorEnvase: v }))}
          hint="Opcional. Si la dejas vacía, en impresión se usará el dato de Dynamics cuando exista."
        />

        <div style={{ display: "flex", gap: 10, flexWrap: "wrap", paddingTop: 4 }}>
          <Button
            appearance="primary"
            onClick={onRegisterAndGenerate}
            disabled={!canRegister}
          >
            {busy ? "Registrando…" : "Registrar y generar QR"}
          </Button>
        </div>
      </AppCard>

      <AppCard style={{ display: "grid", gap: 20, maxWidth: 720 }}>
        <div>
          <Text style={{ fontSize: 15, fontWeight: 600, color: brand.text, display: "block" }}>
            Vista previa
          </Text>
          <Text style={{ fontSize: 13, color: brand.muted, marginTop: 4, display: "block" }}>
            {hasPreview
              ? "Misma disposición que en impresora Zebra. Puedes descargar PNG o ZPL."
              : "Completa el formulario y pulsa «Registrar y generar QR» para ver la etiqueta con código."}
          </Text>
        </div>

        <div
          ref={previewRef}
          style={{
            display: "flex",
            flexDirection: "column",
            alignItems: "center",
            gap: 0,
          }}
        >
          <div
            style={{
              width: "100%",
              maxWidth: 432,
              borderRadius: 10,
              border: `1px solid ${brand.borderStrong}`,
              backgroundColor: brand.surface,
              boxShadow: "0 2px 12px rgba(0,0,0,0.06)",
              padding: 16,
              boxSizing: "border-box",
            }}
          >
            <div
              style={{
                width: "100%",
                maxWidth: 400,
                margin: "0 auto",
                borderRadius: 6,
                overflow: "hidden",
                border: "1px solid rgba(0,0,0,0.12)",
                background: "#fff",
                position: "relative",
                isolation: "isolate",
              }}
            >
              <div
                style={{
                  width: "100%",
                  maxWidth: 400,
                  paddingBottom: "75%",
                  height: 0,
                  position: "relative",
                  overflow: "hidden",
                }}
              >
                <div
                  style={{
                    position: "absolute",
                    top: 0,
                    left: 0,
                    width: 800,
                    height: 600,
                    transform: "scale(0.5)",
                    transformOrigin: "top left",
                    pointerEvents: "none",
                  }}
                >
                  <LabelPreview
                    materialName={form.nombre.trim() || form.lote.trim() || "—"}
                    codigo={form.codigo.trim() || "—"}
                    lote={form.lote.trim() || "—"}
                    fecha={form.fechaEntrada}
                    caducidad={caducidadDisplay}
                    reanalisis={reanalisisDisplay}
                    cantidad={form.cantidadPorEnvase.trim() || "N/A"}
                    envaseNum={form.envaseNum || "—"}
                    envaseTotal={form.envaseTotal || "—"}
                    qrData={qrDataUrl}
                    logoUrl={`${import.meta.env.BASE_URL}logo-olnatura.png`}
                    documentCode={createResp?.label?.documentCode ?? "AL-001-E02/04"}
                  />
                </div>
              </div>
            </div>
          </div>
        </div>

        <div
          style={{
            display: "flex",
            flexWrap: "wrap",
            gap: 10,
            alignItems: "center",
            paddingTop: 4,
            borderTop: `1px solid ${brand.border}`,
          }}
        >
          <Text style={{ fontSize: 13, fontWeight: 600, color: brand.text2, width: "100%", marginBottom: 2 }}>
            Descargas
          </Text>
          <Button
            appearance="outline"
            onClick={onDownloadPng}
            disabled={!canQr || !hasPreview || busy}
          >
            Descargar PNG
          </Button>
          <Button
            appearance="primary"
            onClick={onDownloadZpl}
            disabled={!canQr || !createResp || busy}
          >
            {busy ? "Descargando…" : "Descargar Zebra (.zpl)"}
          </Button>
        </div>

        {hasPreview ? (
          <div style={{ color: brand.successFg, fontSize: 13, background: brand.successBg, padding: "10px 12px", borderRadius: 8 }}>
            Etiqueta registrada correctamente.
          </div>
        ) : null}
      </AppCard>
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
    <div style={{ display: "grid", gap: 8 }}>
      <Text style={{ fontSize: 14, fontWeight: 500, color: brand.text2 }}>{label}</Text>
      <Input appearance="outline" size="large" value={value} onChange={(_, d) => onChange(d.value)} placeholder={placeholder} />
      {hint ? <Text style={{ fontSize: 12, color: brand.muted }}>{hint}</Text> : null}
    </div>
  );
}
