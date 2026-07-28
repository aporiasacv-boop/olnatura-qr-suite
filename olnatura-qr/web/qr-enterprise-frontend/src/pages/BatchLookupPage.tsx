import { useMemo, useState } from "react";
import {
  Button,
  Dialog,
  DialogActions,
  DialogBody,
  DialogContent,
  DialogSurface,
  DialogTitle,
  Input,
  Text,
  Textarea,
  Tooltip,
  makeStyles,
  shorthands,
} from "@fluentui/react-components";
import AppCard from "../components/ui/AppCard";
import { brand } from "../styles/brand";
import { API_BASE, api, ApiError } from "../api/client";
import type { QrResponse, ScanEvent, ApprovalLeg, LoteComment, Role } from "../api/types";
import { useAuth } from "../auth/AuthContext";
import { useToasts } from "../components/ui/toasts";
import LoadingState from "../components/ui/LoadingState";
import EmptyState from "../components/ui/EmptyState";
import ErrorState from "../components/ui/ErrorState";
import StatusTag, {
  normalizeOperationalStatus,
} from "../components/ui/StatusTag";
import { LABELS, fuenteDisplay, formatDateTime, formatLastSyncedAt } from "../utils/displayLabels";
import { displayUserIdentity } from "../utils/auditActionTranslator";
import { formatDateDDMMYYYY } from "../utils/dateFormat";
import { formatNumber, formatQuantity } from "../utils/formatNumber";
import ScanHistoryTable from "../components/ui/ScanHistoryTable";
import LoteAutocomplete from "../components/ui/LoteAutocomplete";

const COMMENT_ROLES: Role[] = ["ADMIN", "ALMACEN", "CALIDAD", "INSPECCION"];
const COMMENT_MAX = 200;

function canUseComments(hasRole: (r: Role) => boolean): boolean {
  return COMMENT_ROLES.some((r) => hasRole(r));
}

function roleDisplay(role: string | null | undefined): string {
  const v = (role ?? "").trim().toUpperCase();
  if (v === "INSPECCION") return "INSPECCIÓN";
  if (v === "CALIDAD") return "CALIDAD";
  if (v === "ALMACEN") return "ALMACÉN";
  if (v === "ADMIN") return "ADMINISTRADOR";
  if (v === "PRODUCCION") return "PRODUCCIÓN";
  return v || "—";
}

type EditForm = {
  tipoMaterial: string;
  nombre: string;
  codigo: string;
  fechaEntrada: string;
  caducidad: string;
  reanalisis: string;
  envaseNum: string;
  envaseTotal: string;
  cantidadPorEnvase: string;
  motivo: string;
};

function needsCalidadApproval(tipo: string): boolean {
  const t = (tipo || "").toUpperCase();
  return t.includes("MATERIA_PRIMA") || t.includes("EMPAQUE_PRIMARIO") || t === "MP";
}

function needsInspeccionApproval(tipo: string): boolean {
  const t = (tipo || "").toUpperCase();
  return t.includes("EMPAQUE_PRIMARIO") || t.includes("EMPAQUE_SECUNDARIO");
}

function asText(v: any, fallback = "—") {
  if (v === null || v === undefined) return fallback;
  if (typeof v === "string") return v.trim() ? v : fallback;
  if (typeof v === "number" || typeof v === "boolean") return String(v);
  return fallback;
}

function readLabel(data: QrResponse | null, key: string, fallback = "—") {
  return asText((data as any)?.label?.[key], fallback);
}

function readDynamic(data: QrResponse | null, key: string, fallback = "—") {
  return asText((data as any)?.dynamic?.[key], fallback);
}

function toEditForm(data: QrResponse): EditForm {
  const label = (data as any)?.label ?? {};
  const fechaEntrada = label.fechaEntrada ? formatDateDDMMYYYY(String(label.fechaEntrada)) : "";
  const caducidad = label.caducidad ? formatDateDDMMYYYY(String(label.caducidad)) : "";
  const reanalisis = label.reanalisis ? formatDateDDMMYYYY(String(label.reanalisis)) : "";
  return {
    tipoMaterial: String(label.tipoMaterial ?? ""),
    nombre: String(label.nombre ?? ""),
    codigo: String(label.codigo ?? ""),
    fechaEntrada: fechaEntrada === "—" ? "" : fechaEntrada,
    caducidad: caducidad === "—" ? "" : caducidad,
    reanalisis: reanalisis === "—" ? "" : reanalisis,
    envaseNum: String(label.envaseNum ?? "1"),
    envaseTotal: String(label.envaseTotal ?? ""),
    cantidadPorEnvase: String(label.cantidadPorEnvase ?? ""),
    motivo: "",
  };
}

async function downloadAuditPdf(
  loteInput: string,
  onError: (msg: string) => void
) {
  const lote = (loteInput ?? "").trim();
  if (!lote) return;

  const base = API_BASE.replace(/\/+$/, "");
  const url = `${base}/api/v1/audit/${encodeURIComponent(lote)}/pdf`;

  try {
    const res = await fetch(url, { method: "GET", credentials: "include" });
    if (!res.ok) {
      onError(res.status === 404 ? "Lote no encontrado." : "No se pudo descargar el PDF.");
      return;
    }
    const blob = await res.blob();
    const href = URL.createObjectURL(blob);
    const a = document.createElement("a");
    const safeLote = (lote || "lote").replace(/[\s/\\]+/g, "_");
    a.href = href;
    a.download = `trazabilidad-${safeLote}.pdf`;
    document.body.appendChild(a);
    a.click();
    a.remove();
    URL.revokeObjectURL(href);
  } catch {
    onError("Error al descargar. Verifica la conexión.");
  }
}

const STATUS_OPTIONS = [
  { value: "CUARENTENA", label: "CUARENTENA" },
  { value: "APROBADO", label: "APROBADO" },
  { value: "RECHAZADO", label: "RECHAZADO" },
] as const;

/** Etiqueta del estado de plataforma (workflow interno / corrección admin). No es Estado Operativo. */
function platformStatusLabel(backendValue: string): string {
  const v = (backendValue ?? "").trim().toUpperCase();
  if (!v || v === "—" || v === "PENDING" || v === "PENDIENTE" || v === "LIBERADO" || v === "OPEN") {
    return "CUARENTENA";
  }
  const opt = STATUS_OPTIONS.find((o) => o.value === v);
  return opt ? opt.label : v;
}

const useStyles = makeStyles({
  page: { display: "grid", gap: "24px" },
  title: { fontSize: "20px", fontWeight: 600, color: brand.text },
  searchCard: {
    ...shorthands.padding("16px"),
    display: "flex",
    gap: "12px",
    alignItems: "flex-end",
  },
  searchInput: { flex: 1 },
  centerGrid: {
    display: "grid",
    gridTemplateColumns: "minmax(0, 7fr) minmax(280px, 3fr)",
    gap: "20px",
    alignItems: "start",
    "@media (max-width: 960px)": {
      gridTemplateColumns: "1fr",
    },
  },
  columnOrderLeft: {
    "@media (max-width: 960px)": {
      order: 2,
    },
  },
  columnOrderRight: {
    "@media (max-width: 960px)": {
      order: 1,
    },
  },
  leftStack: {
    display: "grid",
    gap: "16px",
  },
  rightStack: {
    display: "grid",
    gap: "16px",
  },
  historyFull: { width: "100%" },
  commentList: { display: "grid", gap: "12px", marginTop: "12px" },
  commentCard: {
    ...shorthands.border("1px", "solid", brand.border),
    ...shorthands.borderRadius("10px"),
    ...shorthands.padding("12px"),
    backgroundColor: brand.surface ?? "#fff",
  },
  commentMeta: { color: brand.muted, fontSize: "12px" },
  commentRole: { fontWeight: 700, letterSpacing: "0.02em", marginTop: "4px" },
  commentAuthor: { fontWeight: 600, marginTop: "2px" },
  commentBody: { marginTop: "8px", whiteSpace: "pre-wrap", wordBreak: "break-word" },
  commentForm: { display: "grid", gap: "10px", marginTop: "16px" },
  editGrid: {
    marginTop: "12px",
    display: "grid",
    gridTemplateColumns: "1fr 1fr",
    gap: "10px",
  },
  editFull: { gridColumn: "1 / -1" },
  dataGrid: {
    marginTop: "12px",
    display: "grid",
    gridTemplateColumns: "1fr 1fr",
    gap: "10px",
  },
  fieldBox: {
    ...shorthands.border("1px", "solid", brand.border),
    ...shorthands.borderRadius("10px"),
    ...shorthands.padding("10px"),
  },
  fieldLabel: { color: brand.muted, fontSize: "12px" },
  fieldValue: { marginTop: "4px", fontWeight: 600 },
});

export default function BatchLookupPage() {
  const s = useStyles();
  const { hasRole } = useAuth();
  const toasts = useToasts();

  const [lote, setLote] = useState("");
  const [data, setData] = useState<QrResponse | null>(null);
  const [scans, setScans] = useState<ScanEvent[] | null>(null);
  const [comments, setComments] = useState<LoteComment[] | null>(null);
  const [commentDraft, setCommentDraft] = useState("");
  const [commentBusy, setCommentBusy] = useState(false);
  const [commentComposerOpen, setCommentComposerOpen] = useState(false);
  const [editing, setEditing] = useState(false);
  const [editForm, setEditForm] = useState<EditForm | null>(null);
  const [editBusy, setEditBusy] = useState(false);
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [statusTarget, setStatusTarget] = useState("");
  const [statusMotivo, setStatusMotivo] = useState("");
  const [statusCorrectBusy, setStatusCorrectBusy] = useState(false);
  const [statusConfirmOpen, setStatusConfirmOpen] = useState(false);

  const [status, setStatus] = useState<"idle" | "loading" | "error" | "ok">("idle");
  const [err, setErr] = useState<{ title: string; detail?: string } | null>(null);
  const [statusBusy, setStatusBusy] = useState(false);
  const [syncBusy, setSyncBusy] = useState(false);

  const loteTrim = useMemo(() => lote.trim(), [lote]);
  const commentsAllowed = canUseComments(hasRole);
  const canCorrect =
    !!data?.permissions?.canCorrectLabel || hasRole("ADMIN");

  const load = async (loteOverride?: string) => {
    const key = (loteOverride ?? lote).trim();
    if (!key) return;

    setStatus("loading");
    setErr(null);
    setData(null);
    setScans(null);
    setComments(null);
    setCommentDraft("");
    setCommentComposerOpen(false);
    setEditing(false);
    setEditForm(null);
    setConfirmOpen(false);
    setStatusTarget("");
    setStatusMotivo("");
    setStatusConfirmOpen(false);

    try {
      const qr = await api<QrResponse>(`/qr/${encodeURIComponent(key)}`);
      setData(qr);

      const ev = await api<ScanEvent[]>(`/scan/${encodeURIComponent(key)}`);
      setScans(Array.isArray(ev) ? ev : []);

      if (commentsAllowed) {
        try {
          const list = await api<LoteComment[]>(`/comments/${encodeURIComponent(key)}`);
          setComments(Array.isArray(list) ? list : []);
        } catch {
          setComments([]);
        }
      } else {
        setComments(null);
      }

      setStatus("ok");
    } catch (e) {
      const ae = e as ApiError;
      const isDynamics =
        ae.status === 502 ||
        ae.status === 504 ||
        (typeof ae.body?.error === "string" && String(ae.body.error).startsWith("DYNAMICS_"));

      setErr({
        title:
          ae.status === 404
            ? "Lote no encontrado"
            : isDynamics
              ? "Error de Dynamics 365"
              : "Error al consultar",
        detail:
          ae.status === 404
            ? "Verifica el identificador e intenta de nuevo."
            : ae.status === 401
              ? "Tu sesión expiró. Vuelve a iniciar sesión."
              : isDynamics
                ? ae.message ||
                  "No se pudo obtener datos de Dynamics 365. Intenta de nuevo o contacta a soporte."
                : ae.message || "No se pudo obtener la información del lote.",
      });

      setStatus("error");
    }
  };

  /**
   * Sincronizar con Dynamics: reconsulta OData sin vaciar la UI.
   * Si Dynamics falla, conserva la información anterior.
   */
  const syncWithDynamics = async () => {
    const key = loteTrim;
    if (!key || syncBusy) return;

    setSyncBusy(true);
    try {
      const qr = await api<QrResponse>(`/qr/${encodeURIComponent(key)}/sync-dynamics`, {
        method: "POST",
        toast: false,
      });
      setData(qr);
      setStatus("ok");
      setErr(null);
      toasts.push({
        intent: "success",
        title: "Sincronización completada",
        message: "Se actualizó la información desde Dynamics 365. No se modificó el ERP.",
      });
    } catch (e) {
      const ae = e as ApiError;
      const isDynamics =
        ae.status === 502 ||
        ae.status === 504 ||
        (typeof ae.body?.error === "string" && String(ae.body.error).startsWith("DYNAMICS_"));
      toasts.push({
        intent: "error",
        title: isDynamics ? "No fue posible sincronizar" : "Error al sincronizar",
        message: isDynamics
          ? ae.message ||
            "Dynamics 365 no respondió. Se conservó la información anterior."
          : ae.message || "No se pudo sincronizar. Se conservó la información anterior.",
      });
      // Conservar data/status previos — no limpiar.
    } finally {
      setSyncBusy(false);
    }
  };

  const submitComment = async () => {
    const text = commentDraft.trim();
    if (!loteTrim || !text || commentBusy || !commentsAllowed) return;
    if (text.length > COMMENT_MAX) {
      toasts.push({
        intent: "error",
        title: "Comentario demasiado largo",
        message: `Máximo ${COMMENT_MAX} caracteres.`,
      });
      return;
    }
    setCommentBusy(true);
    try {
      const created = await api<LoteComment>(`/comments/${encodeURIComponent(loteTrim)}`, {
        method: "POST",
        body: { comment: text },
        toast: false,
      });
      setComments((prev) => [...(prev ?? []), created]);
      setCommentDraft("");
      setCommentComposerOpen(false);
      toasts.push({
        intent: "success",
        title: "Comentario registrado",
        message: "Se agregó a la bitácora del lote.",
      });
    } catch (e) {
      const ae = e as ApiError;
      toasts.push({
        intent: "error",
        title: "No se pudo comentar",
        message: ae.message || "Intenta de nuevo.",
      });
    } finally {
      setCommentBusy(false);
    }
  };

  const startEdit = () => {
    if (!data || !canCorrect) return;
    setEditForm(toEditForm(data));
    setEditing(true);
  };

  const cancelEdit = () => {
    setEditing(false);
    setEditForm(null);
    setConfirmOpen(false);
  };

  const applyCorrection = async () => {
    if (!editForm || !loteTrim || editBusy) return;
    const motivo = editForm.motivo.trim();
    if (!motivo) {
      toasts.push({
        intent: "error",
        title: "Motivo obligatorio",
        message: "Indica el motivo de la modificación.",
      });
      return;
    }
    setEditBusy(true);
    try {
      await api(`/admin/lots/by-lote/${encodeURIComponent(loteTrim)}/correct`, {
        method: "PATCH",
        body: {
          motivo,
          tipoMaterial: editForm.tipoMaterial.trim(),
          nombre: editForm.nombre.trim(),
          codigo: editForm.codigo.trim(),
          fechaEntrada: editForm.fechaEntrada.trim(),
          caducidad: editForm.caducidad.trim(),
          reanalisis: editForm.reanalisis.trim(),
          envaseNum: Number.parseInt(editForm.envaseNum, 10) || undefined,
          envaseTotal: Number.parseInt(editForm.envaseTotal, 10) || undefined,
          cantidadPorEnvase: editForm.cantidadPorEnvase,
        },
        toast: false,
      });
      setConfirmOpen(false);
      setEditing(false);
      setEditForm(null);
      toasts.push({
        intent: "success",
        title: "Corrección aplicada",
        message: "Los cambios quedaron registrados en auditoría.",
      });
      await load(loteTrim);
    } catch (e) {
      const ae = e as ApiError;
      toasts.push({
        intent: "error",
        title: "No se pudo corregir",
        message: ae.message || "Intenta de nuevo.",
      });
    } finally {
      setEditBusy(false);
    }
  };

  const applyStatusCorrection = async () => {
    if (!loteTrim || !statusTarget || statusCorrectBusy) return;
    const motivo = statusMotivo.trim();
    if (!motivo) {
      toasts.push({
        intent: "error",
        title: "Motivo obligatorio",
        message: "Indica el motivo de la corrección del estado de plataforma.",
      });
      return;
    }
    setStatusCorrectBusy(true);
    try {
      await api(`/admin/lots/by-lote/${encodeURIComponent(loteTrim)}/correct-status`, {
        method: "PATCH",
        body: { status: statusTarget, motivo },
        toast: false,
      });
      setStatusConfirmOpen(false);
      setStatusMotivo("");
      setStatusTarget("");
      toasts.push({
        intent: "success",
        title: "Corrección de plataforma aplicada",
        message: `Estado de plataforma actualizado a ${statusTarget}. El Estado Operativo (Dynamics) no cambia.`,
      });
      await load(loteTrim);
    } catch (e) {
      const ae = e as ApiError;
      toasts.push({
        intent: "error",
        title: "No se pudo corregir el estado",
        message: ae.message || "Intenta de nuevo.",
      });
    } finally {
      setStatusCorrectBusy(false);
    }
  };

  const changeStatus = async (action: "approve" | "reject") => {
    if (!loteTrim || statusBusy) return;
    setStatusBusy(true);
    try {
      await api(
        action === "approve"
          ? `/label/by-lote/${encodeURIComponent(loteTrim)}/approve`
          : `/label/by-lote/${encodeURIComponent(loteTrim)}/reject`,
        { method: "POST", body: {}, toast: false }
      );
      toasts.push({
        intent: "success",
        title: action === "approve" ? "Aprobación registrada" : "Material rechazado",
        message:
          action === "approve"
            ? "Se actualizó el workflow interno (estado de plataforma). El Estado Operativo (Dynamics) no cambia."
            : "El workflow interno quedó en RECHAZADO. El Estado Operativo (Dynamics) no cambia.",
      });
      await load(loteTrim);
    } catch (e) {
      const ae = e as ApiError;
      toasts.push({
        intent: "error",
        title: "No se pudo actualizar el workflow interno",
        message: ae.message || "Intenta de nuevo.",
      });
    } finally {
      setStatusBusy(false);
    }
  };

  const labelEnvase = useMemo(() => {
    const num = readLabel(data, "envaseNum");
    const total = readLabel(data, "envaseTotal");
    if (num === "—" && total === "—") return "—";
    return `${formatNumber(num)} / ${formatNumber(total)}`;
  }, [data]);

  const fechaEntradaDisplay = useMemo(() => {
    const fromDyn = readDynamic(data, "fechaEntrada");
    if (fromDyn !== "—") return formatDateDDMMYYYY(fromDyn);
    const fromLabel = readLabel(data, "fechaEntrada");
    return fromLabel === "—" ? "—" : formatDateDDMMYYYY(fromLabel);
  }, [data]);

  const caducidadResumen = useMemo(() => {
    const raw = readLabel(data, "caducidad");
    return raw === "—" ? "—" : formatDateDDMMYYYY(raw);
  }, [data]);

  const operationalStatus = String((data as any)?.dynamic?.status ?? "").trim().toUpperCase();
  const operationalRule = String((data as any)?.dynamic?.operationalStatusRule ?? "").trim();
  const platformStatus = String(
    (data as any)?.dynamic?.platformStatus ?? ""
  ).trim().toUpperCase();
  const statusDynamicsRef = (data as any)?.dynamic?.statusDynamics;
  const dynamicCantidad = useMemo(() => {
    const qty = (data as any)?.dynamic?.cantidadAlmacen ?? (data as any)?.dynamic?.cantidad;
    const uom = (data as any)?.dynamic?.unidadInventario ?? (data as any)?.dynamic?.uom;
    return formatQuantity(qty, uom);
  }, [data]);

  const canChangeStatus = data?.permissions?.canChangeStatus ?? false;
  const canApprove =
    !!(data?.permissions?.canApproveCalidad || data?.permissions?.canApproveInspeccion);
  const canReject = !!data?.permissions?.canReject;
  const pendingMessage = data?.permissions?.pendingMessage ?? null;
  const tipoMaterialDisplay = data?.permissions?.tipoMaterialDisplay
    ?? readLabel(data, "tipoMaterial");
  const tipoMaterialCode = String((data as any)?.label?.tipoMaterial ?? "").trim();
  const canDownloadPdf = data?.permissions?.canDownloadAuditPdf
    ?? (hasRole("ADMIN") || hasRole("CALIDAD") || hasRole("INSPECCION"));
  const calidadApproved = !!data?.permissions?.calidadApproved;
  const inspeccionApproved = !!data?.permissions?.inspeccionApproved;
  const calidadLeg = data?.permissions?.calidad;
  const inspeccionLeg = data?.permissions?.inspeccion;
  const allowedStatusCorrections = useMemo(() => {
    const fromApi = data?.permissions?.allowedStatusCorrections;
    if (Array.isArray(fromApi) && fromApi.length > 0) return fromApi;
    if (!hasRole("ADMIN")) return [] as string[];
    const s = platformStatus || "CUARENTENA";
    if (s === "CUARENTENA") return ["APROBADO"];
    if (s === "APROBADO" || s === "RECHAZADO") return ["CUARENTENA"];
    return [] as string[];
  }, [data, hasRole, platformStatus]);
  const canCorrectStatus =
    !!data?.permissions?.canCorrectStatus || (hasRole("ADMIN") && allowedStatusCorrections.length > 0);

  const dynamicFuenteRaw = (data as any)?.dynamic?.fuente ?? "";
  const fuenteDisplayLabel = fuenteDisplay(dynamicFuenteRaw);
  const lastSyncedAtRaw = String((data as any)?.dynamic?.lastSyncedAt ?? "").trim();
  const lastSyncedDisplay = lastSyncedAtRaw ? formatLastSyncedAt(lastSyncedAtRaw) : LABELS.noData;
  const statusSourceDisplay =
    String((data as any)?.dynamic?.statusSource ?? "").trim() ||
    "Dynamics 365 Finance & Operations";

  const sortedComments = useMemo(() => {
    if (!comments) return null;
    return [...comments].sort((a, b) => {
      const ta = new Date(a.createdAt).getTime();
      const tb = new Date(b.createdAt).getTime();
      return (Number.isFinite(tb) ? tb : 0) - (Number.isFinite(ta) ? ta : 0);
    });
  }, [comments]);

  const ruleDisplay =
    operationalRule ||
    (normalizeOperationalStatus(operationalStatus) === "DESCONOCIDO"
      ? "Información insuficiente"
      : "—");

  const handleCopy = async (label: string, value: string) => {
    const v = (value ?? "").toString().trim();
    if (!v) return;
    try {
      await navigator.clipboard.writeText(v);
      toasts.push({
        intent: "success",
        title: "Copiado",
        message: `${label} copiado al portapapeles.`,
      });
    } catch {
      toasts.push({
        intent: "error",
        title: "No se pudo copiar",
        message: "Intenta de nuevo o copia manualmente.",
      });
    }
  };

  const cancelCommentComposer = () => {
    setCommentComposerOpen(false);
    setCommentDraft("");
  };

  return (
    <div className={s.page}>
      <h1 className={s.title}>{LABELS.lookup}</h1>

      <AppCard className={s.searchCard}>
        <form
          style={{ display: "contents" }}
          onSubmit={(e) => {
            e.preventDefault();
            void load();
          }}
        >
          <div className={s.searchInput} style={{ display: "grid", gap: 6 }}>
            <Text style={{ fontSize: 14, fontWeight: 500 }}>Lote</Text>
            <LoteAutocomplete
              id="lote"
              name="lote"
              value={lote}
              onChange={setLote}
              onSelect={(item) => {
                setLote(item.lote);
                void load(item.lote);
              }}
              placeholder="Ej. 251201-MEM0003454"
            />
          </div>

          <Button
            appearance="primary"
            type="submit"
            disabled={!loteTrim || status === "loading"}
          >
            Buscar
          </Button>
        </form>
      </AppCard>

      {status === "loading" && <LoadingState label="Consultando lote…" />}

      {status === "error" && err && (
        <ErrorState title={err.title} detail={err.detail} onRetry={() => void load()} />
      )}

      {status === "ok" && data && (
        <>
          <div className={s.centerGrid}>
            {/* Left: lot info + comments (order 2 on mobile) */}
            <div className={`${s.leftStack} ${s.columnOrderLeft}`}>
              <AppCard>
                <div style={{ display: "flex", justifyContent: "space-between", gap: 12, flexWrap: "wrap" }}>
                  <Text weight="semibold">{LABELS.labelData}</Text>
                  {canCorrect && !editing ? (
                    <Button appearance="secondary" size="small" onClick={startEdit}>
                      Editar (Administrador)
                    </Button>
                  ) : null}
                  {editing ? (
                    <div style={{ display: "flex", gap: 8 }}>
                      <Button appearance="secondary" size="small" onClick={cancelEdit} disabled={editBusy}>
                        Cancelar
                      </Button>
                      <Button
                        appearance="primary"
                        size="small"
                        disabled={editBusy || !(editForm?.motivo ?? "").trim()}
                        onClick={() => setConfirmOpen(true)}
                      >
                        Guardar corrección
                      </Button>
                    </div>
                  ) : null}
                </div>

                {editing && editForm ? (
                  <div className={s.editGrid}>
                    <div>
                      <Text className={s.fieldLabel}>Tipo material</Text>
                      <Input
                        value={editForm.tipoMaterial}
                        onChange={(_, d) => setEditForm((f) => f && { ...f, tipoMaterial: d.value })}
                      />
                    </div>
                    <div>
                      <Text className={s.fieldLabel}>Nombre</Text>
                      <Input
                        value={editForm.nombre}
                        onChange={(_, d) => setEditForm((f) => f && { ...f, nombre: d.value })}
                      />
                    </div>
                    <div>
                      <Text className={s.fieldLabel}>Código</Text>
                      <Input
                        value={editForm.codigo}
                        onChange={(_, d) => setEditForm((f) => f && { ...f, codigo: d.value })}
                      />
                    </div>
                    <div>
                      <Text className={s.fieldLabel}>Fecha entrada (dd/MM/yyyy)</Text>
                      <Input
                        value={editForm.fechaEntrada}
                        onChange={(_, d) => setEditForm((f) => f && { ...f, fechaEntrada: d.value })}
                      />
                    </div>
                    <div>
                      <Text className={s.fieldLabel}>Caducidad</Text>
                      <Input
                        value={editForm.caducidad}
                        onChange={(_, d) => setEditForm((f) => f && { ...f, caducidad: d.value })}
                      />
                    </div>
                    <div>
                      <Text className={s.fieldLabel}>Reanálisis</Text>
                      <Input
                        value={editForm.reanalisis}
                        onChange={(_, d) => setEditForm((f) => f && { ...f, reanalisis: d.value })}
                      />
                    </div>
                    <div>
                      <Text className={s.fieldLabel}>Envase núm.</Text>
                      <Input
                        value={editForm.envaseNum}
                        onChange={(_, d) => setEditForm((f) => f && { ...f, envaseNum: d.value })}
                      />
                    </div>
                    <div>
                      <Text className={s.fieldLabel}>Envases total</Text>
                      <Input
                        value={editForm.envaseTotal}
                        onChange={(_, d) => setEditForm((f) => f && { ...f, envaseTotal: d.value })}
                      />
                    </div>
                    <div className={s.editFull}>
                      <Text className={s.fieldLabel}>Cantidad por envase (incluye unidad si aplica)</Text>
                      <Input
                        value={editForm.cantidadPorEnvase}
                        onChange={(_, d) => setEditForm((f) => f && { ...f, cantidadPorEnvase: d.value })}
                      />
                    </div>
                    <div className={s.editFull}>
                      <Text className={s.fieldLabel}>Motivo de la modificación *</Text>
                      <Textarea
                        value={editForm.motivo}
                        onChange={(_, d) => setEditForm((f) => f && { ...f, motivo: d.value })}
                        placeholder="Ej. Error de captura detectado durante revisión."
                        rows={3}
                        resize="vertical"
                        maxLength={500}
                      />
                    </div>
                    <Text className={s.editFull} style={{ color: brand.muted, fontSize: 12 }}>
                      El inventario y la unidad de Dynamics no se corrigen aquí (solo consulta).
                    </Text>
                  </div>
                ) : (
                  <div className={s.dataGrid}>
                    <Field label="Tipo material" value={readLabel(data, "tipoMaterial")} />
                    <Field label="Nombre" value={readLabel(data, "nombre")} />
                    <CopyField
                      label="Código"
                      value={readLabel(data, "codigo")}
                      onCopy={handleCopy}
                    />
                    <CopyField
                      label="Lote"
                      value={readLabel(data, "lote")}
                      onCopy={handleCopy}
                    />
                    <Field label="Fecha entrada" value={fechaEntradaDisplay} />
                    <Field label="Caducidad" value={readLabel(data, "caducidad")} />
                    <Field label="Reanálisis" value={readLabel(data, "reanalisis")} />
                    <Field label={LABELS.envase} value={labelEnvase} />
                    <Field
                      label="Cantidad por envase"
                      value={formatNumber(readLabel(data, "cantidadPorEnvase"))}
                    />
                  </div>
                )}
              </AppCard>

              <AppCard>
                <Text weight="semibold">{LABELS.comments}</Text>
                <Text style={{ display: "block", marginTop: 4, color: brand.muted, fontSize: 13 }}>
                  Bitácora operativa del lote. Los comentarios no se pueden editar ni eliminar. Máx.{" "}
                  {COMMENT_MAX} caracteres.
                </Text>

                {!commentsAllowed ? (
                  <div style={{ marginTop: 16 }}>
                    <EmptyState title="Tu rol no tiene acceso a la bitácora de comentarios." />
                  </div>
                ) : (
                  <>
                    <div className={s.commentList}>
                      {sortedComments === null ? (
                        <LoadingState label="Cargando comentarios…" />
                      ) : sortedComments.length === 0 ? (
                        <EmptyState title={LABELS.commentsEmpty} />
                      ) : (
                        sortedComments.map((c) => {
                          const when = formatDateTime(c.createdAt);
                          return (
                            <div key={c.id} className={s.commentCard}>
                              <div className={s.commentMeta}>{`${when.date} ${when.time}`}</div>
                              <div className={s.commentAuthor}>
                                {displayUserIdentity(c.displayName, c.username)}
                              </div>
                              <div className={s.commentRole}>{roleDisplay(c.role)}</div>
                              <div className={s.commentBody}>{`"${c.comment}"`}</div>
                            </div>
                          );
                        })
                      )}
                    </div>

                    {commentComposerOpen ? (
                      <div className={s.commentForm}>
                        <Textarea
                          value={commentDraft}
                          onChange={(_, d) => setCommentDraft(d.value.slice(0, COMMENT_MAX))}
                          placeholder={LABELS.commentsPlaceholder}
                          rows={4}
                          resize="vertical"
                          maxLength={COMMENT_MAX}
                        />
                        <Text style={{ color: brand.muted, fontSize: 12 }}>
                          {commentDraft.length}/{COMMENT_MAX}
                        </Text>
                        <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
                          <Button
                            appearance="primary"
                            disabled={!commentDraft.trim() || commentBusy}
                            onClick={() => void submitComment()}
                          >
                            {commentBusy ? "…" : LABELS.commentsSave}
                          </Button>
                          <Button
                            appearance="secondary"
                            disabled={commentBusy}
                            onClick={cancelCommentComposer}
                          >
                            {LABELS.commentsCancel}
                          </Button>
                        </div>
                      </div>
                    ) : (
                      <div style={{ marginTop: 16 }}>
                        <Button appearance="primary" onClick={() => setCommentComposerOpen(true)}>
                          {LABELS.commentsAdd}
                        </Button>
                      </div>
                    )}
                  </>
                )}
              </AppCard>
            </div>

            {/* Right: status + summary + technical (order 1 on mobile) */}
            <div className={`${s.rightStack} ${s.columnOrderRight}`}>
              <AppCard>
                <div
                  style={{
                    display: "flex",
                    justifyContent: "space-between",
                    gap: 12,
                    flexWrap: "wrap",
                    alignItems: "flex-start",
                  }}
                >
                  <Text weight="semibold">{LABELS.dynamicState}</Text>
                  <Tooltip content={LABELS.syncDynamicsHint} relationship="description">
                    <Button
                      appearance="secondary"
                      size="small"
                      disabled={syncBusy || statusBusy}
                      onClick={() => void syncWithDynamics()}
                    >
                      {syncBusy ? LABELS.syncDynamicsBusy : LABELS.syncDynamics}
                    </Button>
                  </Tooltip>
                </div>

                <div style={{ marginTop: 12, display: "grid", gap: 10 }}>
                  <div style={{ display: "flex", alignItems: "center", gap: 10, flexWrap: "wrap" }}>
                    <StatusTag status={operationalStatus} />
                  </div>

                  <div style={{ display: "grid", gap: 2 }}>
                    <Text style={{ fontSize: 12, color: brand.muted }}>
                      {LABELS.lastSyncedAt}
                    </Text>
                    <Text style={{ fontSize: 13, fontWeight: 600 }}>{lastSyncedDisplay}</Text>
                    <Text style={{ fontSize: 12, color: brand.muted }}>
                      {LABELS.statusSource}: {statusSourceDisplay}
                    </Text>
                  </div>

                  <Field label="Tipo de material" value={tipoMaterialDisplay} />

                  {(needsCalidadApproval(tipoMaterialCode) || needsInspeccionApproval(tipoMaterialCode)) && (
                    <div style={{ fontSize: 13, color: brand.text2, display: "grid", gap: 8 }}>
                      {needsCalidadApproval(tipoMaterialCode) ? (
                        <ApprovalLegBlock
                          title="Calidad"
                          approved={calidadApproved}
                          leg={calidadLeg}
                        />
                      ) : null}
                      {needsInspeccionApproval(tipoMaterialCode) ? (
                        <ApprovalLegBlock
                          title="Inspección"
                          approved={inspeccionApproved}
                          leg={inspeccionLeg}
                        />
                      ) : null}
                      {pendingMessage ? (
                        <Text style={{ color: brand.warningFg, fontWeight: 600 }}>{pendingMessage}</Text>
                      ) : null}
                    </div>
                  )}

                  {(canApprove || canReject) && (
                    <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
                      {canApprove ? (
                        <Button
                          appearance="primary"
                          size="small"
                          onClick={() => void changeStatus("approve")}
                          disabled={statusBusy}
                        >
                          {statusBusy ? "…" : "Aprobar (workflow interno)"}
                        </Button>
                      ) : null}
                      {canReject ? (
                        <Button
                          appearance="secondary"
                          size="small"
                          onClick={() => void changeStatus("reject")}
                          disabled={statusBusy}
                        >
                          Rechazar (workflow interno)
                        </Button>
                      ) : null}
                    </div>
                  )}

                  {!canChangeStatus && platformStatus === "CUARENTENA" && !canCorrectStatus ? (
                    <Text style={{ color: brand.muted, fontSize: 12 }}>
                      No tienes permiso para aprobar o rechazar el workflow interno de este material.
                    </Text>
                  ) : null}

                  {(platformStatus === "APROBADO" || platformStatus === "RECHAZADO") && !canCorrectStatus ? (
                    <Text style={{ color: brand.muted, fontSize: 12 }}>
                      El workflow interno del lote es definitivo y no puede modificarse aquí. El Estado
                      Operativo sigue determinado solo por Dynamics.
                    </Text>
                  ) : null}

                  {canCorrectStatus ? (
                    <div
                      style={{
                        border: `1px solid ${brand.border}`,
                        borderRadius: 10,
                        padding: 12,
                        display: "grid",
                        gap: 10,
                        background: brand.surfaceMuted,
                      }}
                    >
                      <Text weight="semibold">Corrección Administrativa</Text>
                      <Text style={{ fontSize: 13 }}>
                        Estado de plataforma actual:{" "}
                        <strong>{platformStatusLabel(platformStatus)}</strong>
                      </Text>
                      <div>
                        <Text className={s.fieldLabel}>Estado de plataforma destino</Text>
                        <div style={{ display: "flex", gap: 8, flexWrap: "wrap", marginTop: 4 }}>
                          {allowedStatusCorrections.map((t) => (
                            <Button
                              key={t}
                              size="small"
                              appearance={statusTarget === t ? "primary" : "secondary"}
                              onClick={() => setStatusTarget(t)}
                              disabled={statusCorrectBusy}
                            >
                              → {t}
                            </Button>
                          ))}
                        </div>
                      </div>
                      <div>
                        <Text className={s.fieldLabel}>Motivo *</Text>
                        <Textarea
                          value={statusMotivo}
                          onChange={(_, d) => setStatusMotivo(d.value)}
                          placeholder='Ej. "Error de liberación detectado."'
                          rows={3}
                          resize="vertical"
                          maxLength={500}
                        />
                      </div>
                      <Button
                        appearance="primary"
                        size="small"
                        disabled={!statusTarget || !statusMotivo.trim() || statusCorrectBusy}
                        onClick={() => setStatusConfirmOpen(true)}
                      >
                        Aplicar corrección de plataforma
                      </Button>
                    </div>
                  ) : null}
                </div>
              </AppCard>

              <AppCard>
                <Text weight="semibold">{LABELS.operationalSummary}</Text>
                <div className={s.dataGrid}>
                  <Field label={LABELS.almacen} value={readDynamic(data, "almacen")} />
                  <Field label={LABELS.ubicacion} value={readDynamic(data, "ubicacion")} />
                  <Field label={LABELS.cantidad} value={dynamicCantidad} />
                  <Field label="Fecha de entrada" value={fechaEntradaDisplay} />
                  <Field label="Caducidad" value={caducidadResumen} />
                  <Field label={LABELS.fuente} value={fuenteDisplayLabel} />
                  <Field label={LABELS.lastSyncedAt} value={lastSyncedDisplay} />
                </div>
              </AppCard>

              <details>
                <summary
                  style={{
                    cursor: "pointer",
                    color: brand.muted,
                    fontSize: 13,
                    fontWeight: 600,
                    userSelect: "none",
                  }}
                >
                  {LABELS.technicalDetails}
                </summary>
                <div style={{ marginTop: 10, display: "grid", gap: 10 }}>
                  <Field
                    label={LABELS.batchDispositionCode}
                    value={readDynamic(data, "batchDispositionCode")}
                  />
                  <Field
                    label={LABELS.passedBatchDispositionCode}
                    value={readDynamic(data, "passedBatchDispositionCode")}
                  />
                  <Field
                    label={LABELS.qualityOrderStatus}
                    value={readDynamic(data, "qualityOrderStatus")}
                  />
                  <Field label={LABELS.statusDynamics} value={asText(statusDynamicsRef)} />
                  <Text style={{ fontSize: 13, color: brand.text2 }}>
                    {LABELS.ruleDeterminedBy}: {ruleDisplay}
                  </Text>
                </div>
              </details>
            </div>
          </div>

          <AppCard className={s.historyFull} style={{ marginTop: 4 }}>
            <Text weight="semibold">{LABELS.scanHistory}</Text>
            <div style={{ marginTop: 12 }}>
              {scans === null ? null : scans.length === 0 ? (
                <EmptyState title={LABELS.noScans} />
              ) : (
                <ScanHistoryTable events={scans} />
              )}
            </div>
            {canDownloadPdf && (
              <div style={{ marginTop: 12 }}>
                <Button
                  appearance="secondary"
                  size="small"
                  onClick={() =>
                    downloadAuditPdf(loteTrim, (msg) =>
                      toasts.push({ intent: "error", title: "Error", message: msg })
                    )
                  }
                >
                  {LABELS.downloadAuditPdf}
                </Button>
              </div>
            )}
          </AppCard>

          <Dialog open={confirmOpen} onOpenChange={(_, d) => setConfirmOpen(!!d.open)}>
            <DialogSurface>
              <DialogBody>
                <DialogTitle>Confirmar modificación</DialogTitle>
                <DialogContent>
                  Se aplicará la corrección administrativa sobre los datos de captura del lote{" "}
                  <strong>{loteTrim}</strong>. El motivo y los valores anteriores/nuevos quedarán
                  en auditoría. ¿Deseas continuar?
                </DialogContent>
                <DialogActions>
                  <Button appearance="secondary" onClick={() => setConfirmOpen(false)} disabled={editBusy}>
                    Cancelar
                  </Button>
                  <Button appearance="primary" onClick={() => void applyCorrection()} disabled={editBusy}>
                    {editBusy ? "Guardando…" : "Confirmar"}
                  </Button>
                </DialogActions>
              </DialogBody>
            </DialogSurface>
          </Dialog>

          <Dialog open={statusConfirmOpen} onOpenChange={(_, d) => setStatusConfirmOpen(!!d.open)}>
            <DialogSurface>
              <DialogBody>
                <DialogTitle>Confirmar corrección administrativa</DialogTitle>
                <DialogContent>
                  Esto <strong>no es una aprobación</strong>. Se corregirá el estado de plataforma del lote{" "}
                  <strong>{loteTrim}</strong> de{" "}
                  <strong>{platformStatusLabel(platformStatus)}</strong> a{" "}
                  <strong>{statusTarget}</strong>. Quedará registrado en auditoría con el motivo
                  indicado. El Estado Operativo (Dynamics) no cambia.
                </DialogContent>
                <DialogActions>
                  <Button
                    appearance="secondary"
                    onClick={() => setStatusConfirmOpen(false)}
                    disabled={statusCorrectBusy}
                  >
                    Cancelar
                  </Button>
                  <Button
                    appearance="primary"
                    onClick={() => void applyStatusCorrection()}
                    disabled={statusCorrectBusy}
                  >
                    {statusCorrectBusy ? "Guardando…" : "Confirmar corrección"}
                  </Button>
                </DialogActions>
              </DialogBody>
            </DialogSurface>
          </Dialog>
        </>
      )}

      {status === "idle" && <EmptyState title={LABELS.readyToLookup} />}
    </div>
  );
}

function ApprovalLegBlock({
  title,
  approved,
  leg,
}: {
  title: string;
  approved: boolean;
  leg?: ApprovalLeg | null;
}) {
  if (!approved && !leg?.approved) {
    return (
      <div style={{ border: `1px solid ${brand.border}`, borderRadius: 10, padding: 10 }}>
        <Text weight="semibold">{title}</Text>
        <Text style={{ display: "block", marginTop: 4, color: brand.muted }}>Pendiente</Text>
      </div>
    );
  }
  const when = formatDateTime(leg?.at ?? null);
  const who = (leg?.actorEmail ?? "").trim() || "—";
  return (
    <div style={{ border: `1px solid ${brand.border}`, borderRadius: 10, padding: 10 }}>
      <Text weight="semibold">{title}</Text>
      <Text style={{ display: "block", marginTop: 4 }}>Aprobada</Text>
      <Text style={{ display: "block", marginTop: 4, color: brand.muted, fontSize: 12 }}>
        {who} · {when.date} {when.time}
      </Text>
    </div>
  );
}

function Field({ label, value }: { label: string; value: string }) {
  const s = useStyles();
  return (
    <div className={s.fieldBox}>
      <div className={s.fieldLabel}>{label}</div>
      <div className={s.fieldValue}>{value}</div>
    </div>
  );
}

function CopyField({
  label,
  value,
  onCopy,
}: {
  label: string;
  value: string;
  onCopy: (label: string, value: string) => void;
}) {
  const s = useStyles();
  return (
    <div className={s.fieldBox}>
      <div className={s.fieldLabel}>{label}</div>
      <div style={{ display: "flex", alignItems: "center", gap: 8, marginTop: 4 }}>
        <div className={s.fieldValue} style={{ marginTop: 0, flex: 1 }}>
          {value}
        </div>
        {value && value !== "—" ? (
          <Tooltip content="Copiar" relationship="label">
            <Button size="small" appearance="subtle" onClick={() => onCopy(label, value)}>
              Copiar
            </Button>
          </Tooltip>
        ) : null}
      </div>
    </div>
  );
}
