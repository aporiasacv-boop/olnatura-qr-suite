import { useMemo, useRef, useState, type ReactNode } from "react";
import {
  Accordion,
  AccordionHeader,
  AccordionItem,
  AccordionPanel,
  Button,
  Text,
  Textarea,
  makeStyles,
  shorthands,
} from "@fluentui/react-components";
import AppCard from "../../components/ui/AppCard";
import EmptyState from "../../components/ui/EmptyState";
import ErrorState from "../../components/ui/ErrorState";
import LoadingState from "../../components/ui/LoadingState";
import LoteAutocomplete from "../../components/ui/LoteAutocomplete";
import ScanHistoryTable from "../../components/ui/ScanHistoryTable";
import { normalizeOperationalStatus } from "../../components/ui/StatusTag";
import { CopyField, PlainField } from "../../components/ui/DataFields";
import {
  LabelPreviewPanel,
  PlatformFieldsBlock,
} from "../../components/consulta/LotePlatformPanels";
import { api, ApiError } from "../../api/client";
import type { LoteComment, QrResponse, ScanEvent } from "../../api/types";
import { useAuth } from "../../auth/AuthContext";
import { useToasts } from "../../components/ui/toasts";
import { brand } from "../../styles/brand";
import { fechaTipoEtiqueta, formatDateDDMMYYYY, storedDateText } from "../../utils/dateFormat";
import {
  formatDateTime,
  formatLastSyncedAt,
  LABELS,
} from "../../utils/displayLabels";
import { displayUserIdentity } from "../../utils/auditActionTranslator";
import { formatNumber } from "../../utils/formatNumber";

type ReasonLine = {
  matched: boolean;
  text: string;
};

type ValidationResponse = {
  lote: string;
  codigo: string | null;
  nombre: string | null;
  caducidad: string | null;
  cantidadAlmacen: number | null;
  cantidadRecibida: number | null;
  unidadInventario: string | null;
  fechaEntrada: string | null;
  almacen: string | null;
  ubicacion: string | null;
  fuente: string | null;
  statusDynamics: string | null;
  qualityOrderStatus: string | null;
  passedBatchDispositionCode: string | null;
  batchDispositionCode: string | null;
  warehouses: string[];
  fechaLiberacion: string | null;
  liberadoPor: string | null;
  operationalStatus: string;
  operationalStatusRule: string | null;
  statusSource: string | null;
  reasons: ReasonLine[];
};

const COMMENT_MAX = 200;

function roleDisplay(role: string | null | undefined): string {
  const v = (role ?? "").trim().toUpperCase();
  if (v === "INSPECCION") return "INSPECCIÓN";
  if (v === "CALIDAD") return "CALIDAD";
  if (v === "ALMACEN") return "ALMACÉN";
  if (v === "ADMIN") return "ADMINISTRADOR";
  if (v === "PRODUCCION") return "PRODUCCIÓN";
  if (v === "VALIDACION") return "VALIDACIÓN";
  return v || "—";
}

function dash(v: string | null | undefined): string {
  const t = (v ?? "").trim();
  return t || "—";
}

function formatMaybeDate(v: string | null | undefined): string {
  if (!v || !v.trim()) return "—";
  const formatted = formatDateDDMMYYYY(v);
  return formatted || v;
}

function statusVisual(status: string): {
  emoji: string;
  label: string;
  bg: string;
  fg: string;
  border: string;
} {
  const s = (status ?? "").trim().toUpperCase();
  if (s === "APROBADO" || s === "PARCIAL") {
    return { emoji: "🟢", label: "APROBADO", bg: "#EAF6EE", fg: "#1B5E35", border: "#B7DFC4" };
  }
  if (s === "RECHAZADO") {
    return { emoji: "🔴", label: "RECHAZADO", bg: "#FDECEC", fg: "#8B1E1E", border: "#F0BABA" };
  }
  if (s === "CUARENTENA") {
    return { emoji: "🟡", label: "CUARENTENA", bg: "#FFF8E1", fg: "#8A6D1D", border: "#F0E0A0" };
  }
  return { emoji: "⚪", label: s || "DESCONOCIDO", bg: "#F3F4F6", fg: "#4B5563", border: "#E5E7EB" };
}

const useStyles = makeStyles({
  wrap: {
    display: "grid",
    gap: "16px",
    maxWidth: "1100px",
  },
  title: {
    fontSize: "20px",
    fontWeight: 700,
    color: brand.text,
    margin: 0,
  },
  formRow: {
    display: "flex",
    gap: "10px",
    flexWrap: "wrap",
    alignItems: "flex-end",
  },
  field: {
    display: "grid",
    gap: "6px",
    flex: "1 1 240px",
  },
  label: {
    fontSize: "12px",
    color: brand.muted,
    fontWeight: 600,
  },
  section: {
    display: "grid",
    gap: "10px",
  },
  sectionHeader: {
    display: "grid",
    gap: "2px",
    paddingBottom: "4px",
    borderBottom: `2px solid ${brand.borderStrong}`,
  },
  sectionTitle: {
    fontSize: "16px",
    fontWeight: 700,
    color: brand.text,
    margin: 0,
  },
  grid: {
    display: "grid",
    gridTemplateColumns: "1fr",
    ...shorthands.gap("14px"),
  },
  statusHero: {
    ...shorthands.padding("12px", "14px"),
    borderRadius: "10px",
    textAlign: "center",
    fontSize: "22px",
    fontWeight: 800,
    letterSpacing: "0.02em",
  },
  reasonList: {
    listStyle: "none",
    margin: 0,
    padding: 0,
    display: "grid",
    gap: "8px",
  },
  reasonItem: {
    fontSize: "14px",
    fontFamily: "Consolas, 'Courier New', monospace",
  },
  commentList: { display: "grid", gap: "12px", marginTop: "12px" },
  commentMeta: { color: brand.muted, fontSize: "12px" },
  commentRole: { fontWeight: 700, letterSpacing: "0.02em", marginTop: "4px" },
  commentAuthor: { fontWeight: 600, marginTop: "2px" },
  commentBody: { marginTop: "8px", whiteSpace: "pre-wrap", wordBreak: "break-word" },
  commentForm: { display: "grid", gap: "10px", marginTop: "16px" },
  accordion: {
    display: "grid",
    gap: "12px",
  },
  accordionItem: {
    ...shorthands.border("1px", "solid", "#D5DCCF"),
    ...shorthands.borderRadius("12px"),
    backgroundColor: "#E6EBE3",
    overflow: "hidden",
  },
  commentAccordionItem: {
    ...shorthands.border("1px", "solid", "#D5DCCF"),
    ...shorthands.borderRadius("10px"),
    backgroundColor: "#E8EDF0",
    overflow: "hidden",
  },
  infoSplit: {
    display: "grid",
    gridTemplateColumns: "repeat(2, minmax(0, 1fr))",
    ...shorthands.gap("14px"),
    marginTop: "12px",
    alignItems: "start",
    "@media (max-width: 860px)": {
      gridTemplateColumns: "1fr",
    },
  },
  rightStack: {
    display: "grid",
    gap: "14px",
    alignContent: "start",
  },
  infoPanel: {
    display: "grid",
    gap: "14px",
    alignContent: "start",
    backgroundColor: "#E6EBE3",
    ...shorthands.borderRadius("12px"),
    ...shorthands.padding("16px", "18px"),
    ...shorthands.border("1px", "solid", "#D5DCCF"),
  },
  infoGroupTitle: {
    fontSize: "13px",
    fontWeight: 700,
    letterSpacing: "0.03em",
    textTransform: "uppercase",
    color: "#3F4A54",
    margin: 0,
    paddingBottom: "4px",
    borderBottom: "1px solid #D0D7C8",
  },
  syncRow: {
    display: "flex",
    justifyContent: "space-between",
    gap: "12px",
    flexWrap: "wrap",
    alignItems: "center",
    marginTop: "10px",
    marginBottom: "2px",
  },
  mismatchBanner: {
    ...shorthands.padding("12px", "14px"),
    ...shorthands.borderRadius("10px"),
    backgroundColor: brand.warningBg,
    color: brand.warningFg,
    border: `1px solid ${brand.borderStrong}`,
    marginTop: "10px",
    display: "grid",
    gap: "6px",
  },
  mismatchTitle: {
    fontSize: "14px",
    fontWeight: 700,
    margin: 0,
  },
  mismatchHint: {
    fontSize: "12px",
    margin: 0,
    opacity: 0.92,
  },
  mismatchList: {
    margin: 0,
    paddingLeft: "18px",
    display: "grid",
    gap: "4px",
    fontSize: "13px",
  },
  techGrid: {
    display: "grid",
    gridTemplateColumns: "repeat(2, minmax(0, 1fr))",
    ...shorthands.gap("8px"),
  },
});

function Section({ title, children }: { title: string; children: ReactNode }) {
  const s = useStyles();
  return (
    <section className={s.section} aria-label={title}>
      <header className={s.sectionHeader}>
        <h2 className={s.sectionTitle}>{title}</h2>
      </header>
      {children}
    </section>
  );
}

export default function OperationalStatusValidationTempPage() {
  const s = useStyles();
  const { push } = useToasts();
  const { me, can, hasRole } = useAuth();
  const canViewComments = can("CONSULTA_LOTE");
  const canCreateComments = !!me?.canCreateLoteComments;

  const [lote, setLote] = useState("");
  const [status, setStatus] = useState<"idle" | "loading" | "error" | "ok">("idle");
  const [err, setErr] = useState<{ title: string; detail?: string } | null>(null);
  const [data, setData] = useState<ValidationResponse | null>(null);

  const [platform, setPlatform] = useState<QrResponse | null>(null);
  const [platformLoading, setPlatformLoading] = useState(false);
  const [syncBusy, setSyncBusy] = useState(false);
  const [reprintBusy, setReprintBusy] = useState(false);
  const [lastSyncedAt, setLastSyncedAt] = useState<string | null>(null);

  const [scans, setScans] = useState<ScanEvent[] | null>(null);

  const [comments, setComments] = useState<LoteComment[] | null>(null);
  const [commentDraft, setCommentDraft] = useState("");
  const [commentBusy, setCommentBusy] = useState(false);
  const commentTextareaRef = useRef<HTMLTextAreaElement | null>(null);

  const loteTrim = useMemo(() => lote.trim(), [lote]);
  const commentLoteKey = useMemo(() => (data?.lote ?? lote).trim(), [data?.lote, lote]);

  const sortedComments = useMemo(() => {
    if (!comments) return null;
    return [...comments].sort((a, b) => {
      const ta = new Date(a.createdAt).getTime();
      const tb = new Date(b.createdAt).getTime();
      return ta - tb;
    });
  }, [comments]);

  const lastSyncedDisplay = useMemo(() => {
    const fromPlatform = String(platform?.dynamic?.lastSyncedAt ?? "").trim();
    const raw = fromPlatform || (lastSyncedAt ?? "").trim();
    return raw ? formatLastSyncedAt(raw) : LABELS.noData;
  }, [platform, lastSyncedAt]);

  const isOperativoAprobado =
    !!data && normalizeOperationalStatus(data.operationalStatus) === "APROBADO";

  const reprintRequired = !!platform?.label?.reprintRequired;
  const platformLabelId = String(platform?.label?.id ?? "").trim();
  const isAdmin = hasRole("ADMIN");

  async function confirmPhysicalReprint() {
    if (!platformLabelId || reprintBusy || !isAdmin) return;
    const ok = window.confirm(
      "¿Confirmas que ya reimprimiste y reemplazaste las etiquetas físicas de este lote?"
    );
    if (!ok) return;
    setReprintBusy(true);
    try {
      await api(`/admin/lots/${platformLabelId}/confirm-reprint`, {
        method: "POST",
        toast: false,
      });
      setPlatform((prev) =>
        prev?.label
          ? { ...prev, label: { ...prev.label, reprintRequired: false } }
          : prev
      );
      push({
        intent: "success",
        title: "Reimpresión confirmada",
        message: "La etiqueta física se considera actualizada.",
      });
    } catch (e) {
      const ae = e as ApiError;
      push({
        intent: "error",
        title: "No se pudo confirmar",
        message: ae?.message ?? "Intenta de nuevo.",
        error: ae,
      });
    } finally {
      setReprintBusy(false);
    }
  }

  async function loadComments(loteKey: string) {
    if (!canViewComments || !loteKey) {
      setComments(null);
      return;
    }
    try {
      const list = await api<LoteComment[]>(`/comments/${encodeURIComponent(loteKey)}`);
      setComments(Array.isArray(list) ? list : []);
    } catch {
      setComments([]);
    }
  }

  async function loadPlatform(loteKey: string) {
    if (!loteKey) {
      setPlatform(null);
      return;
    }
    setPlatformLoading(true);
    setPlatform(null);
    try {
      const qr = await api<QrResponse>(`/qr/${encodeURIComponent(loteKey)}`, { toast: false });
      setPlatform(qr?.label ? qr : null);
    } catch {
      setPlatform(null);
    } finally {
      setPlatformLoading(false);
    }
  }

  async function loadScans(loteKey: string) {
    if (!loteKey) {
      setScans([]);
      return;
    }
    try {
      const ev = await api<ScanEvent[]>(`/scan/${encodeURIComponent(loteKey)}`, { toast: false });
      setScans(Array.isArray(ev) ? ev : []);
    } catch {
      setScans([]);
    }
  }

  function mapConsultaError(e: unknown): { title: string; detail?: string } {
    const ae = e as ApiError;
    const isDynamics =
      ae.status === 502 ||
      ae.status === 504 ||
      (typeof ae.body?.error === "string" && String(ae.body.error).startsWith("DYNAMICS_"));

    return {
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
    };
  }

  async function consultar(loteOverride?: string) {
    const q = (loteOverride ?? lote).trim();
    if (!q) {
      push({ title: "Indica un lote", intent: "warning" });
      return;
    }

    setStatus("loading");
    setErr(null);
    setData(null);
    setPlatform(null);
    setScans(null);
    setComments(null);
    setCommentDraft("");
    setLastSyncedAt(null);

    try {
      const res = await api<ValidationResponse>(
        `/temp/operational-status-validation/${encodeURIComponent(q)}`,
        { toast: false }
      );
      setData(res);
      setLastSyncedAt(new Date().toISOString());
      const key = (res.lote ?? q).trim();
      if (key && key !== loteTrim) setLote(key);
      await Promise.all([loadComments(key), loadPlatform(key), loadScans(key)]);
      setStatus("ok");
    } catch (e) {
      setErr(mapConsultaError(e));
      setStatus("error");
    }
  }

  async function syncWithDynamics() {
    const key = commentLoteKey;
    if (!key || syncBusy) return;

    setSyncBusy(true);
    try {
      const res = await api<ValidationResponse>(
        `/temp/operational-status-validation/${encodeURIComponent(key)}`,
        { toast: false }
      );
      setData(res);

      if (platform?.label) {
        try {
          const qr = await api<QrResponse>(`/qr/${encodeURIComponent(key)}/sync-dynamics`, {
            method: "POST",
            toast: false,
          });
          setPlatform(qr?.label ? qr : null);
          const synced = String(qr?.dynamic?.lastSyncedAt ?? "").trim();
          setLastSyncedAt(synced || new Date().toISOString());
        } catch {
          setLastSyncedAt(new Date().toISOString());
        }
      } else {
        setLastSyncedAt(new Date().toISOString());
      }

      push({
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
      push({
        intent: "error",
        title: isDynamics ? "No fue posible sincronizar" : "Error al sincronizar",
        message: isDynamics
          ? ae.message || "Dynamics 365 no respondió. Se conservó la información anterior."
          : ae.message || "No se pudo sincronizar. Se conservó la información anterior.",
      });
    } finally {
      setSyncBusy(false);
    }
  }

  async function submitComment() {
    const text = commentDraft.trim();
    if (!commentLoteKey || !text || commentBusy || !canCreateComments) return;
    if (text.length > COMMENT_MAX) {
      push({
        intent: "error",
        title: "Comentario demasiado largo",
        message: `Máximo ${COMMENT_MAX} caracteres.`,
      });
      return;
    }
    setCommentBusy(true);
    try {
      const created = await api<LoteComment>(`/comments/${encodeURIComponent(commentLoteKey)}`, {
        method: "POST",
        body: { comment: text },
        toast: false,
      });
      setComments((prev) => [...(prev ?? []), created]);
      setCommentDraft("");
      push({
        intent: "success",
        title: "Comentario registrado",
        message: "Se agregó a la bitácora del lote.",
      });
    } catch (err) {
      const msg =
        err instanceof ApiError
          ? err.message
          : err instanceof Error
            ? err.message
            : "Intenta de nuevo.";
      push({ intent: "error", title: "No se pudo comentar", message: msg });
    } finally {
      setCommentBusy(false);
    }
  }

  const handleCopy = async (label: string, value: string) => {
    const v = (value ?? "").toString().trim();
    if (!v) return;
    try {
      await navigator.clipboard.writeText(v);
      push({
        intent: "success",
        title: "Copiado",
        message: `${label} copiado al portapapeles.`,
      });
    } catch {
      push({
        intent: "error",
        title: "No se pudo copiar",
        message: "Intenta de nuevo o copia manualmente.",
      });
    }
  };

  const visual = data ? statusVisual(data.operationalStatus) : null;
  const qtyRecibida =
    data?.cantidadRecibida != null
      ? `${formatNumber(data.cantidadRecibida)}${data.unidadInventario ? ` ${data.unidadInventario}` : ""}`
      : "—";
  const fechaAprobacionDisplay = (() => {
    const raw = String(data?.fechaLiberacion ?? "").trim();
    if (!raw) return "—";
    return formatLastSyncedAt(raw) || formatMaybeDate(raw);
  })();

  return (
    <div className={s.wrap}>
      <h1 className={s.title}>{LABELS.consultaLote}</h1>

      <AppCard>
        <form
          style={{ display: "contents" }}
          onSubmit={(e) => {
            e.preventDefault();
            void consultar();
          }}
        >
          <div className={s.formRow}>
            <div className={s.field}>
              <Text className={s.label}>Lote</Text>
              <LoteAutocomplete
                id="consulta-lote"
                name="consulta-lote"
                value={lote}
                onChange={setLote}
                onSelect={(item) => {
                  setLote(item.lote);
                  void consultar(item.lote);
                }}
                placeholder="Ej. 260327-MEM0003551"
              />
            </div>
            <Button
              appearance="primary"
              type="submit"
              disabled={!loteTrim || status === "loading"}
            >
              {status === "loading" ? "Consultando…" : "Consultar"}
            </Button>
          </div>
        </form>
      </AppCard>

      {status === "idle" && <EmptyState title={LABELS.readyToLookup} />}

      {status === "loading" && <LoadingState label="Consultando lote…" />}

      {status === "error" && err && (
        <ErrorState title={err.title} detail={err.detail} onRetry={() => void consultar()} />
      )}

      {status === "ok" && data && visual ? (
        <>
          <Section title="Información">
            <div
              className={s.statusHero}
              style={{
                backgroundColor: visual.bg,
                color: visual.fg,
                border: `1px solid ${visual.border}`,
              }}
            >
              {visual.emoji} {visual.label}
            </div>

            {reprintRequired ? (
              <div className={s.mismatchBanner} role="status">
                <p className={s.mismatchTitle}>Etiqueta física desactualizada</p>
                <p className={s.mismatchHint}>
                  Los datos en sistema ya coinciden con Dynamics, pero la etiqueta impresa puede
                  seguir mostrando información anterior. Reimprime, reemplaza las físicas y
                  confirma.
                </p>
                {isAdmin && platformLabelId ? (
                  <div>
                    <Button
                      appearance="secondary"
                      size="small"
                      disabled={reprintBusy}
                      onClick={() => void confirmPhysicalReprint()}
                    >
                      {reprintBusy ? "Confirmando…" : "Confirmar reimpresión física"}
                    </Button>
                  </div>
                ) : (
                  <p className={s.mismatchHint}>
                    Un administrador debe confirmar la reimpresión en Lotes.
                  </p>
                )}
              </div>
            ) : null}

            <div className={s.syncRow}>
              <div style={{ display: "grid", gap: 2 }}>
                <Text style={{ fontSize: 12, color: brand.muted }}>{LABELS.lastSyncedAt}</Text>
                <Text style={{ fontSize: 13, fontWeight: 600 }}>{lastSyncedDisplay}</Text>
              </div>
              <Button
                appearance="secondary"
                size="small"
                disabled={syncBusy}
                onClick={() => void syncWithDynamics()}
              >
                {syncBusy ? LABELS.syncDynamicsBusy : LABELS.syncDynamics}
              </Button>
            </div>

            <div className={s.infoSplit}>
              <div className={s.infoPanel}>
                <h3 className={s.infoGroupTitle}>Dynamics</h3>
                <div className={s.grid}>
                  <CopyField
                    label="Lote"
                    value={dash(data.lote)}
                    onCopy={handleCopy}
                    boxed={false}
                  />
                  <CopyField
                    label="Código / Producto"
                    value={dash(data.codigo)}
                    onCopy={handleCopy}
                    boxed={false}
                  />
                  <PlainField label="Nombre" value={dash(data.nombre)} boxed={false} />
                  <PlainField label={LABELS.almacen} value={dash(data.almacen)} boxed={false} />
                  {fechaTipoEtiqueta(platform?.label?.caducidad, platform?.label?.reanalisis) ==
                  null ? (
                    <PlainField
                      label="Caducidad"
                      value={formatMaybeDate(storedDateText(data.caducidad) || data.caducidad)}
                      boxed={false}
                    />
                  ) : null}
                  <PlainField
                    label="Fecha entrada"
                    value={formatMaybeDate(data.fechaEntrada)}
                    boxed={false}
                  />
                  <PlainField
                    label={LABELS.cantidadRecibida}
                    value={qtyRecibida}
                    boxed={false}
                  />
                  {isOperativoAprobado ? (
                    <PlainField
                      label="Fecha y hora de aprobación"
                      value={fechaAprobacionDisplay}
                      boxed={false}
                    />
                  ) : null}
                </div>
              </div>

              <div className={s.rightStack}>
                <div className={s.infoPanel}>
                  <h3 className={s.infoGroupTitle}>Comentarios</h3>
                  {!canViewComments ? (
                    <EmptyState title="Sin acceso a comentarios" />
                  ) : (
                    <>
                      {sortedComments === null ? (
                        <LoadingState label="Cargando comentarios…" />
                      ) : sortedComments.length === 0 ? (
                        <EmptyState title={LABELS.commentsEmpty} />
                      ) : (
                        <Accordion collapsible className={s.accordion} defaultOpenItems={[]}>
                          {sortedComments.map((c) => {
                            const when = formatDateTime(c.createdAt);
                            const who = displayUserIdentity(c.displayName, c.username);
                            return (
                              <AccordionItem
                                key={c.id}
                                value={String(c.id)}
                                className={s.commentAccordionItem}
                              >
                                <AccordionHeader
                                  size="small"
                                  button={{ style: { fontWeight: 600, fontSize: 13 } }}
                                >
                                  {`${when.date} ${when.time} · ${who}`}
                                </AccordionHeader>
                                <AccordionPanel>
                                  <div className={s.commentRole}>{roleDisplay(c.role)}</div>
                                  <div className={s.commentBody}>{`"${c.comment}"`}</div>
                                </AccordionPanel>
                              </AccordionItem>
                            );
                          })}
                        </Accordion>
                      )}

                      {canCreateComments ? (
                        <div className={s.commentForm}>
                          <Textarea
                            textarea={{
                              ref: commentTextareaRef,
                              id: "consulta-lote-comment-draft",
                            }}
                            value={commentDraft}
                            onChange={(_, d) => setCommentDraft(d.value.slice(0, COMMENT_MAX))}
                            placeholder={LABELS.commentsPlaceholder}
                            rows={3}
                            resize="vertical"
                            maxLength={COMMENT_MAX}
                          />
                          <Text style={{ color: brand.muted, fontSize: 12 }}>
                            {commentDraft.length}/{COMMENT_MAX}
                          </Text>
                          <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
                            <Button
                              appearance="secondary"
                              onClick={() => commentTextareaRef.current?.focus()}
                            >
                              {LABELS.commentsAdd}
                            </Button>
                            <Button
                              appearance="primary"
                              disabled={!commentDraft.trim() || commentBusy}
                              onClick={() => void submitComment()}
                            >
                              {commentBusy ? "…" : LABELS.commentsSave}
                            </Button>
                            <Button
                              appearance="secondary"
                              disabled={commentBusy || !commentDraft}
                              onClick={() => setCommentDraft("")}
                            >
                              {LABELS.commentsCancel}
                            </Button>
                          </div>
                        </div>
                      ) : (
                        <Text style={{ color: brand.muted, fontSize: 12, marginTop: 8 }}>
                          Solo usuarios autorizados por el administrador pueden agregar comentarios.
                        </Text>
                      )}
                    </>
                  )}
                </div>

                <div className={s.infoPanel}>
                  <h3 className={s.infoGroupTitle}>Olnatura QR</h3>
                  {!platformLoading && !platform?.label ? (
                    <Text weight="semibold" style={{ color: "#7A5A12" }}>
                      No se ha generado etiqueta para este lote
                    </Text>
                  ) : (
                    <PlatformFieldsBlock
                      platform={platform}
                      platformLoading={platformLoading}
                      hasRole={hasRole}
                      onToast={push}
                    />
                  )}
                </div>
              </div>
            </div>
          </Section>

          <Section title="Vista previa">
            <LabelPreviewPanel
              platform={platform}
              platformLoading={platformLoading}
              hasRole={hasRole}
              onToast={push}
            />
          </Section>

          <Section title={isAdmin ? "Escaneos y validación" : "Escaneos"}>
            <Accordion collapsible className={s.accordion} defaultOpenItems={[]}>
              <AccordionItem value="escaneos" className={s.accordionItem}>
                <AccordionHeader
                  size="large"
                  button={{ style: { fontWeight: 700, fontSize: 16 } }}
                >
                  {LABELS.scanHistory}
                </AccordionHeader>
                <AccordionPanel>
                  {scans === null ? (
                    <LoadingState label="Cargando historial…" />
                  ) : scans.length === 0 ? (
                    <EmptyState title={LABELS.noScans} />
                  ) : (
                    <ScanHistoryTable events={scans} />
                  )}
                </AccordionPanel>
              </AccordionItem>

              {isAdmin ? (
              <AccordionItem value="validacion-tecnica" className={s.accordionItem}>
                <AccordionHeader
                  size="large"
                  button={{ style: { fontWeight: 700, fontSize: 16 } }}
                >
                  Validación técnica
                </AccordionHeader>
                <AccordionPanel>
                  <div style={{ display: "grid", gap: 14, padding: "4px 2px 8px" }}>
                    <Text weight="semibold" style={{ display: "block" }}>
                      Detalle
                    </Text>
                    <Text
                      style={{
                        display: "block",
                        color: brand.muted,
                        fontSize: 13,
                      }}
                    >
                      Estado: <strong style={{ color: brand.text }}>{visual.label}</strong>
                    </Text>
                    <div className={s.techGrid}>
                      <PlainField
                        label={LABELS.qualityOrderStatus}
                        value={dash(data.qualityOrderStatus)}
                        boxed={false}
                      />
                      <PlainField
                        label={LABELS.batchDispositionCode}
                        value={dash(data.batchDispositionCode)}
                        boxed={false}
                      />
                      <PlainField
                        label={LABELS.passedBatchDispositionCode}
                        value={dash(data.passedBatchDispositionCode)}
                        boxed={false}
                      />
                      <PlainField
                        label={LABELS.statusDynamics}
                        value={dash(data.statusDynamics)}
                        boxed={false}
                      />
                      <PlainField
                        label={LABELS.operationalStatusRule}
                        value={dash(data.operationalStatusRule)}
                        boxed={false}
                      />
                    </div>
                    <Text style={{ display: "block", fontWeight: 600 }}>Razones:</Text>
                    <ul className={s.reasonList}>
                      {(data.reasons ?? []).map((r, i) => (
                        <li
                          key={`${r.text}-${i}`}
                          className={s.reasonItem}
                          style={{ color: r.matched ? "#1B5E35" : brand.muted }}
                        >
                          {r.matched ? "✓" : "✗"} {r.text}
                        </li>
                      ))}
                    </ul>
                  </div>
                </AccordionPanel>
              </AccordionItem>
              ) : null}
            </Accordion>
          </Section>
        </>
      ) : null}
    </div>
  );
}
