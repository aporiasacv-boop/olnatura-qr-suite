import * as React from "react";
import {
  Button,
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
import { api, ApiError } from "../api/client";
import type { ProblemReportItem } from "../api/types";
import { useToasts } from "../components/ui/toasts";
import AppCard from "../components/ui/AppCard";
import { brand } from "../styles/brand";
import {
  TABLE_DATA_CLASS,
  TABLE_FIXED_STYLE,
  TABLE_SCROLL_WRAP,
  WRAP_CELL,
  cellTitle,
} from "../utils/tablePresentation";

const useStyles = makeStyles({
  wrap: { display: "grid", gap: "16px", minWidth: 0, maxWidth: "100%" },
  headerRow: {
    display: "flex",
    justifyContent: "space-between",
    alignItems: "flex-start",
    gap: "12px",
    flexWrap: "wrap",
    marginBottom: "16px",
  },
  title: { fontSize: "20px", fontWeight: 600, color: brand.text, margin: 0 },
  muted: { color: brand.muted },
  filters: {
    display: "flex",
    gap: "8px",
    flexWrap: "wrap",
    alignItems: "center",
  },
  empty: {
    display: "grid",
    placeItems: "center",
    rowGap: "16px",
    ...shorthands.padding("24px"),
  },
  stack: {
    display: "flex",
    flexDirection: "column",
    gap: "2px",
    minWidth: 0,
  },
  primary: {
    fontWeight: 600,
    whiteSpace: "normal",
    overflowWrap: "anywhere",
    wordBreak: "break-word",
    lineHeight: 1.35,
  },
  secondary: {
    fontSize: "12px",
    color: brand.muted,
    whiteSpace: "normal",
    overflowWrap: "anywhere",
    wordBreak: "break-word",
    lineHeight: 1.35,
  },
});

type StatusFilter = "OPEN" | "RESOLVED" | "ALL";

function formatRefreshTime(d: Date | null): string {
  if (!d) return "";
  return d.toLocaleTimeString("es-MX", {
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
  });
}

function formatWhen(iso: string | null | undefined): string {
  if (!iso) return "—";
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  return d.toLocaleString("es-MX", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

function kindLabel(kind: string): string {
  if (kind === "ACCESS") return "Acceso";
  if (kind === "SCAN") return "Escaneo";
  return kind || "—";
}

function statusLabel(status: string): string {
  if (status === "OPEN") return "Pendiente";
  if (status === "RESOLVED") return "Resuelto";
  return status || "—";
}

export default function AdminProblemReportsPage() {
  const s = useStyles();
  const toasts = useToasts();

  const [items, setItems] = React.useState<ProblemReportItem[] | null>(null);
  const [status, setStatus] = React.useState<StatusFilter>("OPEN");
  const [refreshing, setRefreshing] = React.useState(false);
  const [actionId, setActionId] = React.useState<string | null>(null);
  const [lastRefreshedAt, setLastRefreshedAt] = React.useState<Date | null>(null);

  const load = React.useCallback(async () => {
    setRefreshing(true);
    try {
      const qs = status === "ALL" ? "" : `?status=${status}`;
      const all = await api<ProblemReportItem[]>(`/admin/problem-reports${qs}`, { toast: false });
      setItems(all);
      setLastRefreshedAt(new Date());
    } catch (err: unknown) {
      const ae = err as ApiError;
      toasts.push({
        intent: "error",
        title: "No se pudieron cargar reportes",
        message: "Revisa permisos o conexión.",
        error: ae,
      });
      setItems((prev) => (prev === null ? [] : prev));
    } finally {
      setRefreshing(false);
    }
  }, [status, toasts]);

  React.useEffect(() => {
    void load();
  }, [load]);

  const resolve = async (id: string) => {
    if (actionId || refreshing) return;
    setActionId(id);
    try {
      await api<ProblemReportItem>(`/admin/problem-reports/${id}/resolve`, {
        method: "POST",
        toast: false,
      });
      toasts.push({
        intent: "success",
        title: "Marcado como resuelto",
        message: "El reporte quedó cerrado.",
      });
      if (status === "OPEN") {
        setItems((prev) => (prev ?? []).filter((x) => String(x.id) !== id));
      } else {
        await load();
      }
      setLastRefreshedAt(new Date());
    } catch (err: unknown) {
      const ae = err as ApiError;
      toasts.push({
        intent: "error",
        title: "No se pudo resolver",
        message: "Intenta de nuevo.",
        error: ae,
      });
    } finally {
      setActionId(null);
    }
  };

  const rows = items ?? [];
  const refreshLabel = lastRefreshedAt
    ? `Refrescar · ${formatRefreshTime(lastRefreshedAt)}`
    : "Refrescar";

  return (
    <div className={s.wrap}>
      <AppCard>
        <div className={s.headerRow}>
          <div>
            <h1 className={s.title}>Reportes de problemas</h1>
            <Text className={s.muted}>
              Reportes enviados desde la app (escaneo o acceso).
            </Text>
          </div>
          <div className={s.filters}>
            <Button
              appearance={status === "OPEN" ? "primary" : "secondary"}
              onClick={() => setStatus("OPEN")}
              disabled={refreshing || !!actionId}
            >
              Pendientes
            </Button>
            <Button
              appearance={status === "RESOLVED" ? "primary" : "secondary"}
              onClick={() => setStatus("RESOLVED")}
              disabled={refreshing || !!actionId}
            >
              Resueltos
            </Button>
            <Button
              appearance={status === "ALL" ? "primary" : "secondary"}
              onClick={() => setStatus("ALL")}
              disabled={refreshing || !!actionId}
            >
              Todos
            </Button>
            <Button appearance="primary" onClick={() => void load()} disabled={refreshing || !!actionId}>
              {refreshing ? "Actualizando…" : refreshLabel}
            </Button>
          </div>
        </div>

        {items === null ? (
          <div style={{ padding: 24 }}>
            <Text>Cargando…</Text>
          </div>
        ) : rows.length === 0 ? (
          <div className={s.empty}>
            <Text>No hay reportes en este filtro.</Text>
          </div>
        ) : (
          <div style={TABLE_SCROLL_WRAP}>
            <Table className={TABLE_DATA_CLASS} style={TABLE_FIXED_STYLE}>
              <TableHeader>
                <TableRow>
                  <TableHeaderCell>Cuándo</TableHeaderCell>
                  <TableHeaderCell>Tipo</TableHeaderCell>
                  <TableHeaderCell>Motivo</TableHeaderCell>
                  <TableHeaderCell>Detalle</TableHeaderCell>
                  <TableHeaderCell>Estado</TableHeaderCell>
                  <TableHeaderCell>Acción</TableHeaderCell>
                </TableRow>
              </TableHeader>
              <TableBody>
                {rows.map((r) => {
                  const id = String(r.id);
                  const detail = [r.lote ? `Lote: ${r.lote}` : null, r.comment]
                    .filter(Boolean)
                    .join(" · ");
                  return (
                    <TableRow key={id}>
                      <TableCell>
                        <div className={s.stack}>
                          <span className={s.primary}>{formatWhen(r.createdAt)}</span>
                          <span className={s.secondary} title={cellTitle(r.reporterUsername)}>
                            {r.reporterUsername ? `Usuario: ${r.reporterUsername}` : "Sin sesión"}
                          </span>
                        </div>
                      </TableCell>
                      <TableCell>{kindLabel(r.kind)}</TableCell>
                      <TableCell style={WRAP_CELL} title={cellTitle(r.reason)}>
                        {r.reason || "—"}
                      </TableCell>
                      <TableCell style={WRAP_CELL} title={cellTitle(detail)}>
                        {detail || "—"}
                      </TableCell>
                      <TableCell>{statusLabel(r.status)}</TableCell>
                      <TableCell>
                        {r.status === "OPEN" ? (
                          <Button
                            appearance="primary"
                            disabled={!!actionId || refreshing}
                            onClick={() => void resolve(id)}
                          >
                            {actionId === id ? "…" : "Marcar resuelto"}
                          </Button>
                        ) : (
                          <Text className={s.muted}>
                            {r.resolvedByUsername
                              ? `Por ${r.resolvedByUsername}`
                              : formatWhen(r.resolvedAt)}
                          </Text>
                        )}
                      </TableCell>
                    </TableRow>
                  );
                })}
              </TableBody>
            </Table>
          </div>
        )}
      </AppCard>
    </div>
  );
}
