import { useMemo, useRef, useState, type ReactNode } from "react";
import {
  Button,
  Text,
  Textarea,
  Tooltip,
  makeStyles,
  shorthands,
} from "@fluentui/react-components";
import AppCard from "../../components/ui/AppCard";
import EmptyState from "../../components/ui/EmptyState";
import ErrorState from "../../components/ui/ErrorState";
import LoadingState from "../../components/ui/LoadingState";
import LoteAutocomplete from "../../components/ui/LoteAutocomplete";
import ScanHistoryTable from "../../components/ui/ScanHistoryTable";
import StatusTag, { normalizeOperationalStatus } from "../../components/ui/StatusTag";
import { CopyField, PlainField } from "../../components/ui/DataFields";
import {
  LabelPreviewPanel,
  PlatformInfoPanel,
} from "../../components/consulta/LotePlatformPanels";
import { api, ApiError } from "../../api/client";
import type { LoteComment, QrResponse, Role, ScanEvent } from "../../api/types";
import { useAuth } from "../../auth/AuthContext";
import { useToasts } from "../../components/ui/toasts";
import { brand } from "../../styles/brand";
import { formatDateDDMMYYYY } from "../../utils/dateFormat";
import {
  formatDateTime,
  formatLastSyncedAt,
  fuenteDisplay,
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
  if (s === "APROBADO") {
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
    gap: "20px",
    maxWidth: "960px",
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
    gap: "12px",
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
  sectionHint: {
    fontSize: "12px",
    color: brand.muted,
  },
  grid: {
    display: "grid",
    gridTemplateColumns: "repeat(auto-fill, minmax(200px, 1fr))",
    ...shorthands.gap("12px"),
  },
  statusHero: {
    ...shorthands.padding("18px", "16px"),
    borderRadius: "12px",
    textAlign: "center",
    fontSize: "28px",
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
  techBanner: {
    ...shorthands.padding("10px", "12px"),
    borderRadius: "8px",
    backgroundColor: "#FFF4E5",
    ...shorthands.border("1px", "solid", "#F0D9A8"),
    color: "#7A5A12",
    fontSize: "12px",
    lineHeight: 1.4,
  },
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
});

function Section({
  title,
  hint,
  children,
}: {
  title: string;
  hint?: string;
  children: ReactNode;
}) {
  const s = useStyles();
  return (
    <section className={s.section} aria-label={title}>
      <header className={s.sectionHeader}>
        <h2 className={s.sectionTitle}>{title}</h2>
        {hint ? <Text className={s.sectionHint}>{hint}</Text> : null}
      </header>
      {children}
    </section>
  );
}

export default function OperationalStatusValidationTempPage() {
  const s = useStyles();
  const { push } = useToasts();
  const { hasRole } = useAuth();
  const commentsAllowed = canUseComments(hasRole);

  const [lote, setLote] = useState("");
  const [status, setStatus] = useState<"idle" | "loading" | "error" | "ok">("idle");
  const [err, setErr] = useState<{ title: string; detail?: string } | null>(null);
  const [data, setData] = useState<ValidationResponse | null>(null);

  const [platform, setPlatform] = useState<QrResponse | null>(null);
  const [platformLoading, setPlatformLoading] = useState(false);
  const [syncBusy, setSyncBusy] = useState(false);
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

  async function loadComments(loteKey: string) {
    if (!commentsAllowed || !loteKey) {
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
    if (!commentLoteKey || !text || commentBusy || !commentsAllowed) return;
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
  const qty =
    data?.cantidadAlmacen != null
      ? `${formatNumber(data.cantidadAlmacen)}${data.unidadInventario ? ` ${data.unidadInventario}` : ""}`
      : "—";
  const fuenteDisplayLabel = fuenteDisplay(data?.fuente);
  const statusSourceDisplay =
    dash(data?.statusSource) !== "—"
      ? dash(data?.statusSource)
      : "Dynamics 365 Finance & Operations";

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
          <Section
            title="Información de Dynamics"
            hint="Datos obtenidos directamente desde Dynamics."
          >
            <AppCard>
              <div
                style={{
                  display: "flex",
                  justifyContent: "space-between",
                  gap: 12,
                  flexWrap: "wrap",
                  alignItems: "flex-start",
                  marginBottom: 12,
                }}
              >
                <div style={{ display: "flex", alignItems: "center", gap: 10, flexWrap: "wrap" }}>
                  <Text weight="semibold">{LABELS.dynamicState}</Text>
                  <StatusTag status={data.operationalStatus} />
                </div>
                <Tooltip content={LABELS.syncDynamicsHint} relationship="description">
                  <Button
                    appearance="secondary"
                    size="small"
                    disabled={syncBusy}
                    onClick={() => void syncWithDynamics()}
                  >
                    {syncBusy ? LABELS.syncDynamicsBusy : LABELS.syncDynamics}
                  </Button>
                </Tooltip>
              </div>

              <div style={{ display: "grid", gap: 2, marginBottom: 12 }}>
                <Text style={{ fontSize: 12, color: brand.muted }}>{LABELS.lastSyncedAt}</Text>
                <Text style={{ fontSize: 13, fontWeight: 600 }}>{lastSyncedDisplay}</Text>
                <Text style={{ fontSize: 12, color: brand.muted }}>
                  {LABELS.statusSource}: {statusSourceDisplay}
                </Text>
              </div>

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
                <PlainField label="Nombre" value={dash(data.nombre)} />
                <PlainField label={LABELS.almacen} value={dash(data.almacen)} />
                <PlainField label={LABELS.ubicacion} value={dash(data.ubicacion)} />
                <PlainField label="Caducidad" value={formatMaybeDate(data.caducidad)} />
                <PlainField label="Fecha entrada" value={formatMaybeDate(data.fechaEntrada)} />
                <PlainField label={LABELS.cantidad} value={qty} />
                {isOperativoAprobado ? (
                  <>
                    <PlainField
                      label={LABELS.fechaLiberacion}
                      value={formatMaybeDate(data.fechaLiberacion)}
                    />
                    <PlainField label={LABELS.liberadoPor} value={dash(data.liberadoPor)} />
                  </>
                ) : null}
                <PlainField label={LABELS.fuente} value={fuenteDisplayLabel} />
                <PlainField
                  label="Warehouses encontrados"
                  value={
                    data.warehouses && data.warehouses.length > 0
                      ? data.warehouses.join(", ")
                      : "—"
                  }
                />
                <PlainField
                  label={LABELS.qualityOrderStatus}
                  value={dash(data.qualityOrderStatus)}
                />
                <PlainField
                  label={LABELS.batchDispositionCode}
                  value={dash(data.batchDispositionCode)}
                />
                <PlainField
                  label={LABELS.passedBatchDispositionCode}
                  value={dash(data.passedBatchDispositionCode)}
                />
                <PlainField label={LABELS.statusDynamics} value={dash(data.statusDynamics)} />
                <PlainField
                  label={LABELS.operationalStatusRule}
                  value={dash(data.operationalStatusRule)}
                />
              </div>
            </AppCard>
          </Section>

          <Section
            title="Información de la plataforma"
            hint="Datos propios de Olnatura QR (etiqueta, workflow interno, token QR)."
          >
            <PlatformInfoPanel
              platform={platform}
              platformLoading={platformLoading}
              hasRole={hasRole}
              onToast={push}
            />
          </Section>

          <Section title="Comentarios" hint="Bitácora del lote. Independiente de si hay etiqueta registrada.">
            <AppCard>
              <Text style={{ display: "block", color: brand.muted, fontSize: 13 }}>
                Los comentarios no se pueden editar ni eliminar. Máx. {COMMENT_MAX} caracteres.
              </Text>

              {!commentsAllowed ? (
                <div style={{ marginTop: 16 }}>
                  <EmptyState title="Tu rol no tiene acceso a la bitácora de comentarios." />
                </div>
              ) : (
                <>
                  {sortedComments === null ? (
                    <div className={s.commentList}>
                      <LoadingState label="Cargando comentarios…" />
                    </div>
                  ) : sortedComments.length === 0 ? (
                    <div style={{ marginTop: 16 }}>
                      <EmptyState
                        title={LABELS.commentsEmpty}
                        hint="Sé el primero en agregar un comentario a este lote."
                      />
                    </div>
                  ) : (
                    <div className={s.commentList}>
                      {sortedComments.map((c) => {
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
                      })}
                    </div>
                  )}

                  <div className={s.commentForm}>
                    <Textarea
                      textarea={{
                        ref: commentTextareaRef,
                        id: "consulta-lote-comment-draft",
                      }}
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
                </>
              )}
            </AppCard>
          </Section>

          <Section
            title="Vista previa de etiqueta"
            hint="Vista previa y acciones según tu rol. Solo Admin y Almacén pueden descargar PDF e imprimir."
          >
            <LabelPreviewPanel
              platform={platform}
              platformLoading={platformLoading}
              hasRole={hasRole}
              onToast={push}
            />
          </Section>

          <Section title={LABELS.scanHistory} hint="Eventos de escaneo registrados para este lote.">
            <AppCard>
              {scans === null ? (
                <LoadingState label="Cargando historial…" />
              ) : scans.length === 0 ? (
                <EmptyState title={LABELS.noScans} />
              ) : (
                <ScanHistoryTable events={scans} />
              )}
            </AppCard>
          </Section>

          <Section
            title="Panel técnico de validación"
            hint="Herramienta de contraste del OperationalStatusResolver."
          >
            <div className={s.techBanner}>
              Panel técnico temporal. Permite verificar que el Estado Operativo calculado
              coincide con Dynamics. No forma parte del producto final.
            </div>
            <AppCard>
              <Text weight="semibold" style={{ display: "block", marginBottom: 8 }}>
                Resultado del OperationalStatusResolver
              </Text>
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
              <Text style={{ display: "block", marginTop: 10, fontSize: 12, color: brand.muted }}>
                Regla: {dash(data.operationalStatusRule)} · Fuente: {dash(data.statusSource)}
              </Text>
            </AppCard>
            <AppCard>
              <Text weight="semibold" style={{ display: "block", marginBottom: 6 }}>
                Detalle de consulta
              </Text>
              <Text style={{ display: "block", marginBottom: 12, color: brand.muted, fontSize: 13 }}>
                Estado calculado:{" "}
                <strong style={{ color: brand.text }}>{visual.label}</strong>
              </Text>
              <Text style={{ display: "block", marginBottom: 8, fontWeight: 600 }}>Razones:</Text>
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
            </AppCard>
          </Section>
        </>
      ) : null}
    </div>
  );
}
