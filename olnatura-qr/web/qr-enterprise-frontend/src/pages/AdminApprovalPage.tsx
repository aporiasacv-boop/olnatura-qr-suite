import * as React from "react";
import {
  Button,
  Card,
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
import { brand } from "../styles/brand";

const useStyles = makeStyles({
  wrap: {
    display: "grid",
    rowGap: "12px",
  },
  card: {
    ...shorthands.border("1px", "solid", brand.border),
    ...shorthands.borderRadius("14px"),
  },
  headerRow: {
    display: "flex",
    justifyContent: "space-between",
    alignItems: "center",
    gap: "12px",
  },
  muted: { color: brand.muted },
  actions: { display: "flex", gap: "8px" },
  empty: {
    display: "grid",
    placeItems: "center",
    rowGap: "10px",
    ...shorthands.padding("18px"),
    ...shorthands.borderRadius("12px"),
    ...shorthands.border("1px", "solid", brand.border),
    backgroundColor: "#FFF",
  },
});

export default function AdminApprovalPage() {
  const s = useStyles();
  const toasts = useToasts();

  const [items, setItems] = React.useState<AccessRequestItem[] | null>(null);
  const [busy, setBusy] = React.useState(false);

  const load = React.useCallback(async () => {
    setBusy(true);
    try {
      const all = await api<AccessRequestItem[]>("/admin/access-requests");
      setItems(all.filter((x) => x.enabled === false));
    } catch (err: any) {
      const ae = err as ApiError;
      toasts.push({
        intent: "error",
        title: "No se pudo cargar solicitudes",
        message: "Revisa permisos o conexión.",
        error: ae,
      });
      setItems([]);
    } finally {
      setBusy(false);
    }
  }, [toasts]);

  React.useEffect(() => {
    load();
  }, [load]);

  const approve = async (id: string | number) => {
    if (busy) return;
    setBusy(true);
    try {
      await api<void>(`/admin/access-requests/${id}/approve`, { method: "POST" });
      toasts.push({
        intent: "success",
        title: "Aprobado",
        message: `Solicitud ${id} habilitada.`,
      });
      await load();
    } catch (err: any) {
      const ae = err as ApiError;
      toasts.push({
        intent: "error",
        title: "No se pudo aprobar",
        message: "Intenta de nuevo.",
        error: ae,
      });
    } finally {
      setBusy(false);
    }
  };

  const reject = async (id: string | number) => {
    if (busy) return;
    setBusy(true);
    try {
      await api<void>(`/admin/access-requests/${id}/reject`, { method: "POST" });
      toasts.push({
        intent: "success",
        title: "Rechazado",
        message: `Solicitud ${id} rechazada.`,
      });
      await load();
    } catch (err: any) {
      const ae = err as ApiError;
      toasts.push({
        intent: "error",
        title: "No se pudo rechazar",
        message: "Intenta de nuevo.",
        error: ae,
      });
    } finally {
      setBusy(false);
    }
  };

  const pending = items ?? [];

  return (
    <div className={s.wrap}>
      <div className={s.headerRow}>
        <div>
          <Text weight="semibold" size={600}>
            Aprobar usuarios
          </Text>
          <div className={s.muted}>
            <Text size={300}>
              Solicitudes pendientes de acceso (enabled=false)
            </Text>
          </div>
        </div>

        <Button appearance="primary" onClick={load} disabled={busy}>
          {busy ? "Actualizando…" : "Refrescar"}
        </Button>
      </div>

      <Card className={s.card}>
        <div style={{ padding: 12 }}>
          {items === null ? (
            <div style={{ padding: 12 }}>
              <Text>Cargando…</Text>
            </div>
          ) : pending.length === 0 ? (
            <div className={s.empty}>
              <Text weight="semibold">No hay solicitudes pendientes</Text>
              <Text size={300} className={s.muted}>
                Cuando alguien envíe una solicitud, aparecerá aquí.
              </Text>
              <Button appearance="primary" onClick={load} disabled={busy}>
                Refrescar
              </Button>
            </div>
          ) : (
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
                {pending.map((r) => (
                  <TableRow key={String(r.id)}>
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
                          disabled={busy}
                        >
                          Aprobar
                        </Button>
                        <Button
                          appearance="subtle"
                          onClick={() => void reject(r.id)}
                          disabled={busy}
                        >
                          Rechazar
                        </Button>
                      </div>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </div>
      </Card>
    </div>
  );
}