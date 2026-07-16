import { useMemo, useRef, useState, type CSSProperties } from "react";
import {
  Text,
  Input,
  Button,
  Radio,
  RadioGroup,
  Dropdown,
  Option,
  Dialog,
  DialogSurface,
  DialogBody,
  DialogTitle,
  DialogContent,
  DialogActions,
  Link,
} from "@fluentui/react-components";
import AppCard from "../components/ui/AppCard";
import { brand } from "../styles/brand";
import { useAuth } from "../auth/AuthContext";
import { api, ApiError } from "../api/client";
import { downloadLabelZplFile } from "../utils/downloadLabelZpl";
import { generateQrPlain } from "../utils/qrWithLogo";
import { formatDateDDMMYYYY, isValidDDMMYYYY } from "../utils/dateFormat";
import type { DynamicsLookupResponse } from "../api/types";
import LabelPreview from "../components/label/LabelPreview";
import LoteAutocomplete from "../components/ui/LoteAutocomplete";
import type { FechaTipo } from "../utils/labelToPng";
import {
  dynamicsSiteFamily,
  dynamicsSiteLabel,
  extractDynamicsSiteCode,
  materialCategoryDisplay,
  type DynamicsSiteFamily,
} from "../utils/dynamicsMaterialMap";

const QR_PREFIX = "OLNQR:1:";
const ENVASE_INICIO = 1;

const DYNAMICS_BG = "#f8f9fa";
const REQUIRED_BORDER = "#f3b5b5";

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

/** Campos precargados desde Dynamics (UI only). */
type DynamicsLocked = Partial<Record<keyof FormState, boolean>>;

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
    envaseNum: "1",
    envaseTotal: "",
    cantidadPorEnvase: "",
  });

  const [busy, setBusy] = useState(false);
  const [lookupBusy, setLookupBusy] = useState(false);
  const [dynamicsInfo, setDynamicsInfo] = useState<DynamicsLookupResponse | null>(null);
  const [dynamicsLocked, setDynamicsLocked] = useState<DynamicsLocked>({});
  /** Familia Dynamics (MPM/MPS vs MEM/MES) que condiciona el selector de tipo. */
  const [siteFamily, setSiteFamily] = useState<DynamicsSiteFamily>("DESCONOCIDO");
  const [siteCode, setSiteCode] = useState<string | null>(null);
  const [qrDataUrl, setQrDataUrl] = useState<string | null>(null);
  const [createResp, setCreateResp] = useState<CreateResponse | null>(null);
  const [err, setErr] = useState<string | null>(null);
  const [zplHelpOpen, setZplHelpOpen] = useState(false);

  /** Siempre empezamos en envase 1; el operador solo captura el total. */
  const loteOk = form.lote.trim().length > 0;
  const fechaEntradaOk = isValidDDMMYYYY(form.fechaEntrada);
  const envaseTotal = parseInt(form.envaseTotal, 10) || 0;
  const envaseOk = envaseTotal >= 1;
  const canRegister = canQr && loteOk && fechaEntradaOk && envaseOk && !!form.tipoMaterial.trim() && !busy;

  const caducidadDisplay = form.fechaTipo === "CADUCIDAD" ? form.fechaValor : "";
  const reanalisisDisplay = form.fechaTipo === "REANALISIS" ? form.fechaValor : "";

  const onConsultDynamics = async () => {
    const lote = form.lote.trim();
    if (!lote || lookupBusy) return;

    setErr(null);
    setLookupBusy(true);
    setDynamicsInfo(null);
    setDynamicsLocked({});
    setSiteFamily("DESCONOCIDO");
    setSiteCode(null);

    try {
      const data = await api<DynamicsLookupResponse>(
        `/dynamics/lookup/${encodeURIComponent(lote)}`,
        { toast: false }
      );
      setDynamicsInfo(data);

      const codigo = data.codigo?.trim() || "";
      const nombre = data.nombre?.trim() || "";
      const loteDyn = data.lote?.trim() || lote;
      const caducidadFmt = data.caducidad ? formatDateDDMMYYYY(data.caducidad) : "";

      const code = extractDynamicsSiteCode(data.almacen, loteDyn);
      const family = dynamicsSiteFamily(data.almacen, loteDyn);
      setSiteCode(code);
      setSiteFamily(family);

      // MPM/MPS → Materia Prima fija. MEM/MES → solo elegir primario/secundario.
      const tipoFromDynamics =
        family === "MATERIA_PRIMA" ? "MATERIA_PRIMA" : family === "EMPAQUE" ? "" : undefined;

      setDynamicsLocked({
        tipoMaterial: family === "MATERIA_PRIMA",
        codigo: !!codigo,
        nombre: !!nombre,
        lote: !!loteDyn,
        // Dynamics solo entrega BatchExpirationDate (fecha), sin indicar si es
        // caducidad o reanálisis → el tipo queda siempre elegible.
        fechaTipo: false,
        fechaValor: !!caducidadFmt,
      });

      setForm((s) => ({
        ...s,
        tipoMaterial:
          tipoFromDynamics !== undefined
            ? tipoFromDynamics
            : family === "EMPAQUE" &&
                (s.tipoMaterial === "EMPAQUE_PRIMARIO" || s.tipoMaterial === "EMPAQUE_SECUNDARIO")
              ? s.tipoMaterial
              : family === "EMPAQUE"
                ? ""
                : s.tipoMaterial,
        codigo: codigo || s.codigo,
        nombre: nombre || s.nombre,
        lote: loteDyn || s.lote,
        fechaTipo: caducidadFmt ? "CADUCIDAD" : s.fechaTipo,
        fechaValor: caducidadFmt || s.fechaValor,
      }));
    } catch (e) {
      const ae = e as ApiError;
      const isDynamics =
        ae.status === 502 ||
        ae.status === 504 ||
        (typeof ae.body?.error === "string" && String(ae.body.error).startsWith("DYNAMICS_"));

      setErr(
        ae.status === 404
          ? "Lote no encontrado en Dynamics. Verifica el identificador."
          : isDynamics
            ? ae.message || "Dynamics 365 no disponible. Intenta de nuevo."
            : ae.message || "No se pudo consultar Dynamics."
      );
    } finally {
      setLookupBusy(false);
    }
  };

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
    if (!form.tipoMaterial.trim()) {
      setErr(
        siteFamily === "EMPAQUE"
          ? "Selecciona si el empaque es primario o secundario."
          : "Selecciona el tipo de material."
      );
      return;
    }
    if (siteFamily === "EMPAQUE" && form.tipoMaterial === "MATERIA_PRIMA") {
      setErr("Para MEM/MES debes elegir Empaque Primario o Secundario.");
      return;
    }
    if (siteFamily === "MATERIA_PRIMA" && form.tipoMaterial !== "MATERIA_PRIMA") {
      setErr("Para MPM/MPS el tipo de material es Materia Prima.");
      return;
    }
    if (!fechaEntradaOk) {
      setErr("Fecha de entrada requerida (DD/MM/YYYY).");
      return;
    }
    if (!envaseOk) {
      setErr("Captura la cantidad total de envases (mínimo 1).");
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
        envaseNum: ENVASE_INICIO,
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

  const onDownloadZpl = async () => {
    setErr(null);
    if (!createResp?.id) {
      setErr("Primero registra la etiqueta para descargar ZPL.");
      return;
    }
    try {
      setBusy(true);
      const total = Math.max(1, envaseTotal);
      await downloadLabelZplFile({
        labelIdOrLote: String(createResp.id),
        totalEnvases: total,
        printFrom: ENVASE_INICIO,
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
        <form
          style={{ display: "grid", gap: 16 }}
          onSubmit={(e) => {
            e.preventDefault();
            void onRegisterAndGenerate();
          }}
        >
        <Text style={{ fontSize: 15, fontWeight: 600, color: brand.text }}>Datos de la etiqueta</Text>
        <div style={{ display: "grid", gap: 6 }}>
          <FieldLabel
            label="Tipo material (aprobación)"
            fromDynamics={siteFamily === "MATERIA_PRIMA"}
          />
          {siteFamily === "MATERIA_PRIMA" ? (
            <Input
              value="Materia Prima"
              readOnly
              style={{ maxWidth: 420, background: DYNAMICS_BG }}
            />
          ) : (
            <Dropdown
              placeholder={
                siteFamily === "EMPAQUE"
                  ? "Primario o secundario"
                  : "Selecciona el tipo"
              }
              value={materialCategoryDisplay(form.tipoMaterial)}
              selectedOptions={form.tipoMaterial ? [form.tipoMaterial] : []}
              onOptionSelect={(_, d) =>
                setForm((s) => ({ ...s, tipoMaterial: (d.optionValue as string) || "" }))
              }
              style={{ maxWidth: 420 }}
            >
              {siteFamily !== "EMPAQUE" ? (
                <Option value="MATERIA_PRIMA">Materia Prima</Option>
              ) : null}
              <Option value="EMPAQUE_PRIMARIO">Material de Empaque Primario</Option>
              <Option value="EMPAQUE_SECUNDARIO">Material de Empaque Secundario</Option>
            </Dropdown>
          )}
          <Text style={{ fontSize: 12, color: brand.muted }}>
            {siteFamily === "MATERIA_PRIMA"
              ? `${dynamicsSiteLabel(siteCode)} → Materia Prima (Calidad aprueba).`
              : siteFamily === "EMPAQUE"
                ? `${dynamicsSiteLabel(siteCode)} → elige primario (Calidad + Inspección) o secundario (Inspección).`
                : "Define quién puede aprobar. Con Dynamics: MPM/MPS = Materia Prima; MEM/MES = elegir primario/secundario."}
          </Text>
        </div>
        <Field
          label="Nombre"
          placeholder="Nombre de material"
          value={form.nombre}
          onChange={(v) => setForm((s) => ({ ...s, nombre: v }))}
          fromDynamics={!!dynamicsLocked.nombre}
        />
        <Field
          label="Código"
          placeholder="Código interno"
          value={form.codigo}
          onChange={(v) => setForm((s) => ({ ...s, codigo: v }))}
          fromDynamics={!!dynamicsLocked.codigo}
        />
        <div style={{ display: "grid", gap: 8 }}>
          <FieldLabel label="Lote" fromDynamics={!!dynamicsLocked.lote} />
          <div style={{ display: "flex", gap: 10, flexWrap: "wrap", alignItems: "center" }}>
            <LoteAutocomplete
              appearance="outline"
              size="large"
              value={form.lote}
              readOnly={!!dynamicsLocked.lote}
              onChange={(loteVal) => {
                if (dynamicsLocked.lote) return;
                if (!dynamicsInfo) {
                  const code = extractDynamicsSiteCode(null, loteVal);
                  const family = dynamicsSiteFamily(null, loteVal);
                  setSiteCode(code);
                  setSiteFamily(family);
                  if (family === "MATERIA_PRIMA") {
                    setForm((s) => ({ ...s, lote: loteVal, tipoMaterial: "MATERIA_PRIMA" }));
                    setDynamicsLocked((lk) => ({ ...lk, tipoMaterial: true }));
                  } else if (family === "EMPAQUE") {
                    setForm((s) => ({
                      ...s,
                      lote: loteVal,
                      tipoMaterial:
                        s.tipoMaterial === "EMPAQUE_PRIMARIO" || s.tipoMaterial === "EMPAQUE_SECUNDARIO"
                          ? s.tipoMaterial
                          : "",
                    }));
                    setDynamicsLocked((lk) => ({ ...lk, tipoMaterial: false }));
                  } else {
                    setForm((s) => ({ ...s, lote: loteVal }));
                    setDynamicsLocked((lk) => ({ ...lk, tipoMaterial: false }));
                  }
                } else {
                  setForm((s) => ({ ...s, lote: loteVal }));
                }
              }}
              onSelect={(item) => {
                if (dynamicsLocked.lote) return;
                setForm((s) => ({ ...s, lote: item.lote }));
              }}
              onKeyDown={(e) => {
                if (e.key === "Enter") {
                  e.preventDefault();
                  void onConsultDynamics();
                }
              }}
              placeholder="Captura el lote y consulta Dynamics"
              style={{
                flex: "1 1 220px",
                ...inputLook({
                  fromDynamics: !!dynamicsLocked.lote,
                  needsAttention: !dynamicsLocked.lote && !form.lote.trim(),
                }),
              }}
            />
            <Button
              appearance="secondary"
              type="button"
              onClick={() => void onConsultDynamics()}
              disabled={!form.lote.trim() || lookupBusy || busy}
            >
              {lookupBusy ? "Consultando…" : "Consultar Dynamics"}
            </Button>
          </div>
          <Text style={{ fontSize: 12, color: brand.muted }}>
            Busca en ItemBatches, inventario y órdenes de calidad para precargar el formulario.
          </Text>
        </div>
        {dynamicsInfo ? (
          <div
            style={{
              fontSize: 13,
              color: brand.text2,
              background: brand.primarySoft,
              padding: "10px 12px",
              borderRadius: 8,
              display: "grid",
              gap: 4,
            }}
          >
            <Text weight="semibold">Datos de Dynamics</Text>
            {dynamicsInfo.statusDynamics ? (
              <Text>Estado calidad: {dynamicsInfo.statusDynamics}</Text>
            ) : null}
            {dynamicsInfo.almacen ? <Text>Almacén: {dynamicsInfo.almacen}</Text> : null}
            {siteCode ? (
              <Text>
                Sitio Dynamics: {dynamicsSiteLabel(siteCode)}
                {siteFamily === "MATERIA_PRIMA"
                  ? " → categoría: Materia Prima"
                  : siteFamily === "EMPAQUE"
                    ? " → elige Empaque Primario o Secundario"
                    : ""}
              </Text>
            ) : null}
            {dynamicsInfo.ubicacion ? <Text>Ubicación: {dynamicsInfo.ubicacion}</Text> : null}
          </div>
        ) : null}
        <Field
          label="Fecha entrada (DD/MM/YYYY)"
          placeholder="Captura fecha de entrada"
          value={form.fechaEntrada}
          onChange={(v) => setForm((s) => ({ ...s, fechaEntrada: v }))}
          fromDynamics={!!dynamicsLocked.fechaEntrada}
          requiredPending={!dynamicsLocked.fechaEntrada}
          isFilled={fechaEntradaOk}
        />
        <div style={{ display: "grid", gap: 6 }}>
          <FieldLabel label="Fecha (tipo)" />
          <RadioGroup
            value={form.fechaTipo}
            onChange={(_, d) => {
              setForm((s) => ({ ...s, fechaTipo: d.value as FechaTipo }));
            }}
            layout="horizontal"
          >
            <Radio value="CADUCIDAD" label="Caducidad" />
            <Radio value="REANALISIS" label="Reanálisis" />
          </RadioGroup>
          <Text size={200} style={{ color: brand.muted }}>
            Elige si la fecha corresponde a caducidad o a reanálisis.
          </Text>
        </div>
        <Field
          label={
            form.fechaTipo === "CADUCIDAD"
              ? "Caducidad (DD/MM/YYYY)"
              : "Reanálisis (DD/MM/YYYY)"
          }
          placeholder={
            form.fechaTipo === "CADUCIDAD"
              ? "Captura fecha de caducidad"
              : "Captura fecha de reanálisis"
          }
          value={form.fechaValor}
          onChange={(v) => setForm((s) => ({ ...s, fechaValor: v }))}
          fromDynamics={!!dynamicsLocked.fechaValor}
        />
        <Field
          label="Cantidad total de envases"
          placeholder="Ej. 30"
          value={form.envaseTotal}
          onChange={(v) => setForm((s) => ({ ...s, envaseTotal: v, envaseNum: "1" }))}
          hint="Se generarán etiquetas del 1 hasta este total (ej. 30 → 30 etiquetas ZPL)."
          requiredPending
          isFilled={envaseTotal >= 1}
        />
        <Field
          label="Cantidad por envase"
          placeholder="Captura cantidad por envase"
          value={form.cantidadPorEnvase}
          onChange={(v) => setForm((s) => ({ ...s, cantidadPorEnvase: v }))}
          hint="Solo captura manual (va en la etiqueta ZPL). El inventario Dynamics se ve al escanear el QR."
          requiredPending
          isFilled={form.cantidadPorEnvase.trim().length > 0}
        />

        <div style={{ display: "flex", gap: 10, flexWrap: "wrap", paddingTop: 4 }}>
          <Button
            appearance="primary"
            type="submit"
            disabled={!canRegister}
          >
            {busy ? "Registrando…" : "Registrar y generar QR"}
          </Button>
        </div>
        </form>
      </AppCard>

      <AppCard style={{ display: "grid", gap: 20, maxWidth: 720 }}>
        <div>
          <Text style={{ fontSize: 15, fontWeight: 600, color: brand.text, display: "block" }}>
            Vista previa
          </Text>
          <Text style={{ fontSize: 13, color: brand.muted, marginTop: 4, display: "block" }}>
            {hasPreview
              ? "Vista previa del QR para escanear desde la computadora. Para impresora Zebra usa el archivo .zpl."
              : "Completa el formulario y pulsa «Registrar y generar QR» para ver el código."}
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
                    envaseNum="1"
                    envaseTotal={form.envaseTotal || "—"}
                    qrData={qrDataUrl}
                    logoUrl={`${import.meta.env.BASE_URL}logo-olnatura.png`}
                    documentCode={createResp?.label?.documentCode ?? "AL-001-E02/04"}
                  />
                </div>
              </div>
            </div>
          </div>
          {hasPreview && qrDataUrl ? (
            <div
              style={{
                marginTop: 16,
                display: "flex",
                flexDirection: "column",
                alignItems: "center",
                gap: 8,
                padding: 16,
                borderRadius: 10,
                border: `1px dashed ${brand.border}`,
                background: "#fff",
              }}
            >
              <Text style={{ fontSize: 13, fontWeight: 600, color: brand.text2 }}>
                QR para escanear desde la computadora
              </Text>
              <img
                src={qrDataUrl}
                alt="Código QR de la etiqueta"
                width={220}
                height={220}
                style={{ display: "block", imageRendering: "pixelated" }}
              />
            </div>
          ) : null}
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
            Impresión Zebra
          </Text>
          <Button
            appearance="primary"
            onClick={onDownloadZpl}
            disabled={!canQr || !createResp || busy}
          >
            {busy ? "Descargando…" : "Descargar Zebra (.zpl)"}
          </Button>
          <Link onClick={() => setZplHelpOpen(true)} style={{ fontSize: 13 }}>
            Cómo imprimir
          </Link>
        </div>

        {hasPreview ? (
          <div style={{ color: brand.successFg, fontSize: 13, background: brand.successBg, padding: "10px 12px", borderRadius: 8 }}>
            Etiqueta registrada correctamente.
          </div>
        ) : null}
      </AppCard>

      <Dialog open={zplHelpOpen} onOpenChange={(_, data) => setZplHelpOpen(data.open)}>
        <DialogSurface>
          <DialogBody>
            <DialogTitle>Cómo imprimir archivos ZPL</DialogTitle>
            <DialogContent>
              <ul style={{ paddingLeft: 18, margin: "8px 0 0 0" }}>
                <li>Descarga el archivo .zpl y guárdalo en tu equipo.</li>
                <li>
                  En equipos con impresora Zebra, envía el archivo al puerto de la impresora
                  (por ejemplo, arrastrando el archivo a la impresora o usando utilidades de Zebra).
                </li>
                <li>
                  No intentes abrir el archivo como documento; es código de comandos para la
                  impresora.
                </li>
              </ul>
            </DialogContent>
            <DialogActions>
              <Button appearance="primary" onClick={() => setZplHelpOpen(false)}>
                Cerrar
              </Button>
            </DialogActions>
          </DialogBody>
        </DialogSurface>
      </Dialog>
    </div>
  );
}

function inputLook({
  fromDynamics,
  needsAttention,
}: {
  fromDynamics?: boolean;
  needsAttention?: boolean;
}): CSSProperties {
  if (fromDynamics) {
    return {
      backgroundColor: DYNAMICS_BG,
      border: `1px solid ${brand.border}`,
      borderRadius: 8,
    };
  }
  if (needsAttention) {
    return {
      border: `1px solid ${REQUIRED_BORDER}`,
      borderRadius: 8,
    };
  }
  return {};
}

function FieldLabel({ label, fromDynamics }: { label: string; fromDynamics?: boolean }) {
  return (
    <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
      <Text style={{ fontSize: 14, fontWeight: 500, color: brand.text2 }}>{label}</Text>
      {fromDynamics ? (
        <Text
          style={{
            fontSize: 11,
            fontWeight: 600,
            color: brand.muted,
            background: DYNAMICS_BG,
            border: `1px solid ${brand.border}`,
            borderRadius: 999,
            padding: "1px 8px",
            letterSpacing: 0.2,
          }}
        >
          Dynamics
        </Text>
      ) : null}
    </div>
  );
}

function Field({
  label,
  placeholder,
  value,
  onChange,
  hint,
  fromDynamics,
  requiredPending,
  isFilled,
}: {
  label: string;
  placeholder: string;
  value: string;
  onChange: (v: string) => void;
  hint?: string;
  fromDynamics?: boolean;
  requiredPending?: boolean;
  isFilled?: boolean;
}) {
  const needsAttention = !!requiredPending && !fromDynamics && !isFilled;

  return (
    <div style={{ display: "grid", gap: 8 }}>
      <FieldLabel label={label} fromDynamics={fromDynamics} />
      <Input
        appearance="outline"
        size="large"
        value={value}
        readOnly={!!fromDynamics}
        onChange={(_, d) => {
          if (fromDynamics) return;
          onChange(d.value);
        }}
        placeholder={placeholder}
        style={inputLook({ fromDynamics, needsAttention })}
      />
      {hint ? <Text style={{ fontSize: 12, color: brand.muted }}>{hint}</Text> : null}
    </div>
  );
}
