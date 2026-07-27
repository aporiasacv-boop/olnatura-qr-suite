import * as React from "react";
import {
  Button,
  Dropdown,
  Input,
  Option,
  makeStyles,
  shorthands,
  Table,
  TableBody,
  TableCell,
  TableHeader,
  TableHeaderCell,
  TableRow,
  Text,
} from "@fluentui/react-components";
import { API_BASE, api, ApiError } from "../api/client";
import { useToasts } from "../components/ui/toasts";
import AppCard from "../components/ui/AppCard";
import AuditDetailCell from "../components/ui/AuditDetailCell";
import LoteAutocomplete from "../components/ui/LoteAutocomplete";
import { brand } from "../styles/brand";
import { LABELS, formatDateTime, actionTypeDisplay, roleDisplay } from "../utils/displayLabels";
import { AUDIT_ACTION_FILTER_OPTIONS, resolveUserDisplay } from "../utils/auditActionTranslator";

const useStyles = makeStyles({
  wrap: { display: "grid", gap: "20px" },
  title: { fontSize: "20px", fontWeight: 600, color: brand.text, margin: 0 },
  headerRow: {
    display: "flex",
    justifyContent: "space-between",
    alignItems: "flex-start",
    gap: "12px",
    flexWrap: "wrap",
  },
  headerActions: { display: "flex", gap: "8px", flexWrap: "wrap" },
  filters: {
    display: "grid",
    gridTemplateColumns: "repeat(auto-fill, minmax(180px, 1fr))",
    ...shorthands.gap("12px"),
    alignItems: "end",
  },
  field: { display: "grid", gap: "6px" },
  label: { fontSize: "13px", fontWeight: 600, color: brand.text2 },
  muted: { color: brand.muted },
  pager: {
    marginTop: "12px",
    display: "flex",
    gap: "8px",
    alignItems: "center",
    flexWrap: "wrap",
  },
});

type AuditEvent = {
  id: string;
  createdAt: string;
  actorId?: string;
  actorEmail?: string;
  actorRol?: string;
  actorDisplay?: string;
  actorRoleDisplay?: string;
  actionType: string;
  actionTypeDisplay?: string;
  lote?: string;
  metadata?: Record<string, unknown>;
};

type Filters = {
  from: string;
  to: string;
  actor: string;
  actionType: string;
  lote: string;
};

function buildQuery(filters: Filters, page: number, size = 30): string {
  const params = new URLSearchParams();
  params.set("page", String(page));
  params.set("size", String(size));
  if (filters.from) params.set("from", filters.from);
  if (filters.to) params.set("to", filters.to);
  if (filters.actor.trim()) params.set("actor", filters.actor.trim());
  if (filters.actionType) params.set("actionType", filters.actionType);
  if (filters.lote.trim()) params.set("lote", filters.lote.trim());
  return params.toString();
}

export default function AdminAuditPage() {
  const s = useStyles();
  const toasts = useToasts();
  const [events, setEvents] = React.useState<AuditEvent[]>([]);
  const [busy, setBusy] = React.useState(false);
  const [exporting, setExporting] = React.useState(false);
  const [page, setPage] = React.useState(0);
  const [totalPages, setTotalPages] = React.useState(0);
  const [draft, setDraft] = React.useState<Filters>({
    from: "",
    to: "",
    actor: "",
    actionType: "",
    lote: "",
  });
  const [applied, setApplied] = React.useState<Filters>(draft);

  const load = React.useCallback(async () => {
    setBusy(true);
    try {
      const res = await api<{ content: AuditEvent[]; totalPages: number }>(
        `/audit?${buildQuery(applied, page)}`,
        { toast: false }
      );
      const data = res as { content?: AuditEvent[]; totalPages?: number };
      setEvents(Array.isArray(data?.content) ? data.content : []);
      setTotalPages(data?.totalPages ?? 0);
    } catch (err) {
      const ae = err as ApiError;
      toasts.push({
        intent: "error",
        title: "Error al cargar historial",
        message: ae?.message ?? "Revisa permisos.",
        error: ae,
      });
      setEvents([]);
    } finally {
      setBusy(false);
    }
  }, [applied, page, toasts]);

  React.useEffect(() => {
    void load();
  }, [load]);

  const applyFilters = () => {
    setPage(0);
    setApplied({ ...draft });
  };

  const clearFilters = () => {
    const empty: Filters = { from: "", to: "", actor: "", actionType: "", lote: "" };
    setDraft(empty);
    setPage(0);
    setApplied(empty);
  };

  const exportCsv = async () => {
    setExporting(true);
    try {
      const params = new URLSearchParams();
      if (applied.from) params.set("from", applied.from);
      if (applied.to) params.set("to", applied.to);
      if (applied.actor.trim()) params.set("actor", applied.actor.trim());
      if (applied.actionType) params.set("actionType", applied.actionType);
      if (applied.lote.trim()) params.set("lote", applied.lote.trim());
      const base = API_BASE.replace(/\/+$/, "");
      const qs = params.toString();
      const url = `${base}/api/v1/audit/export${qs ? `?${qs}` : ""}`;
      const res = await fetch(url, { method: "GET", credentials: "include" });
      if (!res.ok) {
        throw new Error(res.status === 403 ? "Sin permiso" : `HTTP ${res.status}`);
      }
      const blob = await res.blob();
      const href = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = href;
      const cd = res.headers.get("Content-Disposition") || "";
      const match = /filename="?([^"]+)"?/i.exec(cd);
      a.download = match?.[1] || `auditoria.csv`;
      document.body.appendChild(a);
      a.click();
      a.remove();
      URL.revokeObjectURL(href);
      toasts.push({ intent: "success", title: "CSV descargado", message: "Exportación lista." });
    } catch (err) {
      toasts.push({
        intent: "error",
        title: "No se pudo exportar CSV",
        message: err instanceof Error ? err.message : "Intenta de nuevo.",
      });
    } finally {
      setExporting(false);
    }
  };

  const actionLabel =
    AUDIT_ACTION_FILTER_OPTIONS.find((o) => o.value === draft.actionType)?.label ?? "Todas";

  return (
    <div className={s.wrap}>
      <div className={s.headerRow}>
        <h1 className={s.title}>{LABELS.auditLog}</h1>
        <div className={s.headerActions}>
          <Button appearance="secondary" onClick={() => void exportCsv()} disabled={busy || exporting}>
            {exporting ? "Exportando…" : "Exportar CSV"}
          </Button>
          <Button appearance="primary" onClick={() => void load()} disabled={busy}>
            {busy ? "Cargando…" : "Actualizar"}
          </Button>
        </div>
      </div>

      <AppCard>
        <Text weight="semibold" style={{ display: "block", marginBottom: 12 }}>
          Filtros
        </Text>
        <form
          className={s.filters}
          onSubmit={(e) => {
            e.preventDefault();
            applyFilters();
          }}
        >
          <div className={s.field}>
            <span className={s.label}>Desde</span>
            <Input
              type="date"
              value={draft.from}
              onChange={(_, d) => setDraft((f) => ({ ...f, from: d.value }))}
            />
          </div>
          <div className={s.field}>
            <span className={s.label}>Hasta</span>
            <Input
              type="date"
              value={draft.to}
              onChange={(_, d) => setDraft((f) => ({ ...f, to: d.value }))}
            />
          </div>
          <div className={s.field}>
            <span className={s.label}>Usuario</span>
            <Input
              placeholder="Correo o rol"
              value={draft.actor}
              onChange={(_, d) => setDraft((f) => ({ ...f, actor: d.value }))}
            />
          </div>
          <div className={s.field}>
            <span className={s.label}>Acción</span>
            <Dropdown
              placeholder="Todas"
              value={actionLabel}
              selectedOptions={draft.actionType ? [draft.actionType] : [""]}
              onOptionSelect={(_, data) =>
                setDraft((f) => ({ ...f, actionType: data.optionValue ?? "" }))
              }
            >
              {AUDIT_ACTION_FILTER_OPTIONS.map((o) => (
                <Option key={o.value || "all"} value={o.value}>
                  {o.label}
                </Option>
              ))}
            </Dropdown>
          </div>
          <div className={s.field}>
            <span className={s.label}>Lote</span>
            <LoteAutocomplete
              size="medium"
              placeholder="Ej. 260422-…"
              value={draft.lote}
              onChange={(v) => setDraft((f) => ({ ...f, lote: v }))}
              onSelect={(item) => setDraft((f) => ({ ...f, lote: item.lote }))}
            />
          </div>
          <div className={s.field} style={{ display: "flex", gap: 8, alignItems: "end" }}>
            <Button appearance="primary" type="submit" disabled={busy}>
              Aplicar
            </Button>
            <Button appearance="subtle" type="button" onClick={clearFilters} disabled={busy}>
              Limpiar
            </Button>
          </div>
        </form>
      </AppCard>

      <AppCard>
        <div style={{ overflowX: "auto" }}>
          {events.length === 0 ? (
            <div style={{ padding: 24, textAlign: "center", color: brand.muted }}>
              {LABELS.noRecords}
            </div>
          ) : (
            <Table aria-label={LABELS.auditLog}>
              <TableHeader>
                <TableRow>
                  <TableHeaderCell>{LABELS.fecha}</TableHeaderCell>
                  <TableHeaderCell>{LABELS.accion}</TableHeaderCell>
                  <TableHeaderCell>{LABELS.usuario}</TableHeaderCell>
                  <TableHeaderCell>{LABELS.rol}</TableHeaderCell>
                  <TableHeaderCell>Lote</TableHeaderCell>
                  <TableHeaderCell>{LABELS.detalle}</TableHeaderCell>
                </TableRow>
              </TableHeader>
              <TableBody>
                {events.map((e) => {
                  const { date, time } = formatDateTime(e.createdAt);
                  const dateTimeStr =
                    date !== LABELS.noData && time !== LABELS.noData ? `${date} ${time}` : "—";
                  return (
                    <TableRow key={e.id} className="table-hover-row">
                      <TableCell>{dateTimeStr}</TableCell>
                      <TableCell>
                        {e.actionTypeDisplay ?? actionTypeDisplay(e.actionType)}
                      </TableCell>
                      <TableCell>
                        {resolveUserDisplay(e.actorDisplay, undefined, e.actorEmail)}
                      </TableCell>
                      <TableCell>
                        {e.actorRoleDisplay ?? roleDisplay(e.actorRol)}
                      </TableCell>
                      <TableCell>{e.lote ?? "-"}</TableCell>
                      <TableCell>
                        <AuditDetailCell metadata={e.metadata} actionType={e.actionType} />
                      </TableCell>
                    </TableRow>
                  );
                })}
              </TableBody>
            </Table>
          )}
          {totalPages > 1 && (
            <div className={s.pager}>
              <Button
                appearance="subtle"
                disabled={page <= 0 || busy}
                onClick={() => setPage((p) => Math.max(0, p - 1))}
              >
                Anterior
              </Button>
              <Text size={300}>
                Página {page + 1} de {totalPages}
              </Text>
              <Button
                appearance="subtle"
                disabled={page >= totalPages - 1 || busy}
                onClick={() => setPage((p) => p + 1)}
              >
                Siguiente
              </Button>
            </div>
          )}
        </div>
      </AppCard>
    </div>
  );
}
