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
import { useToasts } from "../components/ui/toasts";
import { brand } from "../styles/brand";

const useStyles = makeStyles({
  wrap: { display: "grid", rowGap: "12px" },
  card: {
    ...shorthands.border("1px", "solid", brand.border),
    ...shorthands.borderRadius("14px"),
  },
  headerRow: {
    display: "flex",
    justifyContent: "space-between",
    alignItems: "center",
    gap: "12px",
    flexWrap: "wrap",
  },
  muted: { color: brand.muted },
});

type AuditEvent = {
  id: string;
  createdAt: string;
  actorId?: string;
  actorEmail?: string;
  actorRol?: string;
  actionType: string;
  lote?: string;
  metadata?: Record<string, unknown>;
  deviceId?: string;
};

export default function AdminAuditPage() {
  const s = useStyles();
  const toasts = useToasts();
  const [events, setEvents] = React.useState<AuditEvent[]>([]);
  const [busy, setBusy] = React.useState(false);
  const [page, setPage] = React.useState(0);
  const [totalPages, setTotalPages] = React.useState(0);

  const load = React.useCallback(async () => {
    setBusy(true);
    try {
      const res = await api<{ content: AuditEvent[]; totalPages: number }>(
        `/audit?page=${page}&size=30`,
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
  }, [page, toasts]);

  React.useEffect(() => {
    load();
  }, [load]);

  return (
    <div className={s.wrap}>
      <div className={s.headerRow}>
        <div>
          <Text weight="semibold" size={600}>
            Historial de auditoría
          </Text>
          <div className={s.muted}>
            <Text size={300}>Acciones registradas (append-only)</Text>
          </div>
        </div>
        <Button appearance="primary" onClick={load} disabled={busy}>
          {busy ? "Cargando…" : "Actualizar"}
        </Button>
      </div>

      <Card className={s.card}>
        <div style={{ padding: 12, overflowX: "auto" }}>
          {events.length === 0 ? (
            <div style={{ padding: 24, textAlign: "center", color: brand.muted }}>
              No hay eventos aún
            </div>
          ) : (
            <Table aria-label="Audit events">
              <TableHeader>
                <TableRow>
                  <TableHeaderCell>Fecha</TableHeaderCell>
                  <TableHeaderCell>Acción</TableHeaderCell>
                  <TableHeaderCell>Actor</TableHeaderCell>
                  <TableHeaderCell>Lote</TableHeaderCell>
                  <TableHeaderCell>Detalles</TableHeaderCell>
                </TableRow>
              </TableHeader>
              <TableBody>
                {events.map((e) => (
                  <TableRow key={e.id}>
                    <TableCell>
                      {e.createdAt ? new Date(e.createdAt).toLocaleString() : "-"}
                    </TableCell>
                    <TableCell>{e.actionType}</TableCell>
                    <TableCell>
                      {e.actorRol ?? "-"} {e.actorEmail ? `(${e.actorEmail})` : ""}
                    </TableCell>
                    <TableCell>{e.lote ?? "-"}</TableCell>
                    <TableCell>
                      {e.metadata && Object.keys(e.metadata).length > 0
                        ? JSON.stringify(e.metadata)
                        : "-"}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
          {totalPages > 1 && (
            <div style={{ marginTop: 12, display: "flex", gap: 8 }}>
              <Button
                appearance="subtle"
                disabled={page <= 0 || busy}
                onClick={() => setPage((p) => Math.max(0, p - 1))}
              >
                Anterior
              </Button>
              <Text size={300}>Página {page + 1} de {totalPages}</Text>
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
      </Card>
    </div>
  );
}
