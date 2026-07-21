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
import type { AccessRequestItem } from "../api/types";
import { useToasts } from "../components/ui/toasts";
import AppCard from "../components/ui/AppCard";
import { brand } from "../styles/brand";

const useStyles = makeStyles({
  wrap: { display: "grid", gap: "16px" },
  headerRow: {
    display: "flex",
    justifyContent: "space-between",
    alignItems: "flex-start",
    gap: "12px",
    flexWrap: "wrap",
    marginBottom: "16px",
  },
  title: { fontSize: "20px", fontWeight: 600, color: brand.text, margin: 0 },
  subtitle: { fontSize: "14px", color: brand.muted, marginTop: "4px" },
  muted: { color: brand.muted },
  actions: {
    display: "flex",
    flexDirection: "column",
    alignItems: "flex-start",
    gap: "6px",
  },
  reject: {
    color: brand.dangerFg,
    minHeight: "auto",
    padding: "0 4px",
    fontWeight: 600,
  },
  empty: {
    display: "grid",
    placeItems: "center",
    rowGap: "16px",
    ...shorthands.padding("24px"),
  },
});

function formatRefreshTime(d: Date | null): string {
  if (!d) return "";
  return d.toLocaleTimeString("es-MX", {
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
  });
}

export default function AdminApprovalPage() {
  const s = useStyles();
  const toasts = useToasts();

  const [items, setItems] = React.useState<AccessRequestItem[] | null>(null);
  const [refreshing, setRefreshing] = React.useState(false);
  const [actionId, setActionId] = React.useState<string | null>(null);
  const [lastRefreshedAt, setLastRefreshedAt] = React.useState<Date | null>(null);

  const load = React.useCallback(async () => {
    setRefreshing(true);
    try {
      const all = await api<AccessRequestItem[]>("/admin/access-requests", { toast: false });
      setItems(all.filter((x) => x.enabled === false));
      setLastRefreshedAt(new Date());
    } catch (err: unknown) {
      const ae = err as ApiError;
      toasts.push({
        intent: "error",
        title: "No se pudo cargar solicitudes",
        message: "Revisa permisos o conexión.",
        error: ae,
      });
      setItems((prev) => (prev === null ? [] : prev));
    } finally {
      setRefreshing(false);
    }
  }, [toasts]);

  React.useEffect(() => {
    void load();
  }, [load]);

  const approve = async (id: string | number) => {
    if (actionId || refreshing) return;
    const key = String(id);
    setActionId(key);
    try {
      await api<void>(`/admin/access-requests/${id}/approve`, { method: "POST", toast: false });
      toasts.push({
        intent: "success",
        title: "Aprobado",
        message: `Usuario habilitado.`,
      });
      setItems((prev) => (prev ?? []).filter((x) => String(x.id) !== key));
      setLastRefreshedAt(new Date());
    } catch (err: unknown) {
      const ae = err as ApiError;
      toasts.push({
        intent: "error",
        title: "No se pudo aprobar",
        message: "Intenta de nuevo.",
        error: ae,
      });
    } finally {
      setActionId(null);
    }
  };

  const reject = async (id: string | number) => {
    if (actionId || refreshing) return;
    const key = String(id);
    setActionId(key);
    try {
      await api<void>(`/admin/access-requests/${id}/reject`, { method: "POST", toast: false });
      toasts.push({
        intent: "success",
        title: "Rechazado",
        message: `Solicitud rechazada.`,
      });
      setItems((prev) => (prev ?? []).filter((x) => String(x.id) !== key));
      setLastRefreshedAt(new Date());
    } catch (err: unknown) {
      const ae = err as ApiError;
      toasts.push({
        intent: "error",
        title: "No se pudo rechazar",
        message: "Intenta de nuevo.",
        error: ae,
      });
    } finally {
      setActionId(null);
    }
  };

  const pending = items ?? [];
  const refreshLabel = lastRefreshedAt
    ? `Refrescar · ${formatRefreshTime(lastRefreshedAt)}`
    : "Refrescar";

  return (
    <div className={s.wrap}>
      <AppCard>
        <div className={s.headerRow}>
          <div>
            <h1 className={s.title}>Aprobar usuarios</h1>
          </div>
          <Button appearance="primary" onClick={() => void load()} disabled={refreshing || !!actionId}>
            {refreshing ? "Actualizando…" : refreshLabel}
          </Button>
        </div>

        {items === null ? (
          <div style={{ padding: 24 }}>
            <Text>Cargando…</Text>
          </div>
        ) : pending.length === 0 ? (
          <div className={s.empty}>
            <Text weight="semibold">No hay solicitudes pendientes</Text>
            <Text size={300} className={s.muted}>
              Cuando alguien envíe una solicitud, aparecerá aquí.
            </Text>
            <Button appearance="primary" onClick={() => void load()} disabled={refreshing}>
              {refreshing ? "Actualizando…" : refreshLabel}
            </Button>
          </div>
        ) : (
          <div style={{ overflowX: "auto" }}>
            <Table aria-label="Solicitudes de acceso">
              <TableHeader>
                <TableRow>
                  <TableHeaderCell>ID</TableHeaderCell>
                  <TableHeaderCell>Usuario</TableHeaderCell>
                  <TableHeaderCell>Correo</TableHeaderCell>
                  <TableHeaderCell>Rol</TableHeaderCell>
                  <TableHeaderCell>Creado</TableHeaderCell>
                  <TableHeaderCell>Acciones</TableHeaderCell>
                </TableRow>
              </TableHeader>

              <TableBody>
                {pending.map((r) => {
                  const rowBusy = actionId === String(r.id);
                  return (
                    <TableRow key={String(r.id)} className="table-hover-row">
                      <TableCell>{String(r.id)}</TableCell>
                      <TableCell>{r.username}</TableCell>
                      <TableCell>{r.email}</TableCell>
                      <TableCell>{r.role}</TableCell>
                      <TableCell>
                        {r.createdAt ? new Date(r.createdAt).toLocaleString() : "-"}
                      </TableCell>
                      <TableCell>
                        <div className={s.actions}>
                          <Button
                            appearance="primary"
                            onClick={() => void approve(r.id)}
                            disabled={!!actionId || refreshing}
                          >
                            {rowBusy ? "…" : "Aprobar"}
                          </Button>
                          <Button
                            appearance="transparent"
                            className={s.reject}
                            onClick={() => void reject(r.id)}
                            disabled={!!actionId || refreshing}
                          >
                            Rechazar
                          </Button>
                        </div>
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
