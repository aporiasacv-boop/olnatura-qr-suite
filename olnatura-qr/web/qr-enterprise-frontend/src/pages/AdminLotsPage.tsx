import * as React from "react";
import {
  Button,
  Dropdown,
  makeStyles,
  Option,
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
import AppCard from "../components/ui/AppCard";
import { brand } from "../styles/brand";

type LotAdmin = {
  id: string;
  lote: string;
  codigo: string;
  nombre: string;
  adminStatus: string;
  adminStatusDisplay: string;
  workflowStatus?: string;
  createdAt?: string;
};

const useStyles = makeStyles({
  wrap: { display: "grid", gap: "16px" },
  headerRow: {
    display: "flex",
    justifyContent: "space-between",
    alignItems: "flex-start",
    gap: "12px",
    flexWrap: "wrap",
  },
  title: { fontSize: "20px", fontWeight: 600, color: brand.text, margin: 0 },
  subtitle: { fontSize: "14px", color: brand.muted, marginTop: "4px" },
  sectionTitle: { fontSize: "16px", fontWeight: 600, color: brand.text, margin: "0 0 12px" },
  filterHint: { fontSize: "13px", color: brand.muted, marginTop: "10px", lineHeight: 1.45 },
  filterRow: { display: "grid", gap: "8px", maxWidth: "320px" },
  muted: { color: brand.muted },
  actions: { display: "flex", flexDirection: "column", alignItems: "flex-start", gap: "6px" },
  danger: { color: brand.dangerFg, fontWeight: 600, minHeight: "auto", padding: "0 4px" },
  linkBtn: { minHeight: "auto", padding: "0 4px", fontWeight: 600 },
});

function formatRefreshTime(d: Date | null): string {
  if (!d) return "";
  return d.toLocaleTimeString("es-MX", { hour: "2-digit", minute: "2-digit", second: "2-digit" });
}

export default function AdminLotsPage() {
  const s = useStyles();
  const toasts = useToasts();
  const [filter, setFilter] = React.useState<string>("");
  const [items, setItems] = React.useState<LotAdmin[] | null>(null);
  const [refreshing, setRefreshing] = React.useState(false);
  const [actionId, setActionId] = React.useState<string | null>(null);
  const [lastRefreshedAt, setLastRefreshedAt] = React.useState<Date | null>(null);

  const load = React.useCallback(async () => {
    setRefreshing(true);
    try {
      const qs = filter ? `?adminStatus=${encodeURIComponent(filter)}` : "";
      const res = await api<LotAdmin[]>(`/admin/lots${qs}`, { toast: false });
      setItems(Array.isArray(res) ? res : []);
      setLastRefreshedAt(new Date());
    } catch (err) {
      const ae = err as ApiError;
      toasts.push({
        intent: "error",
        title: "No se pudo cargar lotes",
        message: ae?.message ?? "Revisa permisos.",
        error: ae,
      });
      setItems((prev) => (prev === null ? [] : prev));
    } finally {
      setRefreshing(false);
    }
  }, [filter, toasts]);

  React.useEffect(() => {
    void load();
  }, [load]);

  const setAdminStatus = async (id: string, adminStatus: string) => {
    if (actionId) return;
    setActionId(id);
    try {
      const updated = await api<LotAdmin>(`/admin/lots/${id}/admin-status`, {
        method: "PATCH",
        body: { adminStatus },
        toast: false,
      });
      setItems((prev) => {
        const list = (prev ?? []).map((x) => (x.id === id ? updated : x));
        if (filter && updated.adminStatus !== filter) {
          return list.filter((x) => x.id !== id);
        }
        return list;
      });
      setLastRefreshedAt(new Date());
      toasts.push({
        intent: "success",
        title: "Estado actualizado",
        message: `${updated.lote} → ${updated.adminStatusDisplay}`,
      });
    } catch (err) {
      const ae = err as ApiError;
      toasts.push({
        intent: "error",
        title: "No se pudo cambiar el estado",
        message: ae?.message ?? "Intenta de nuevo.",
        error: ae,
      });
    } finally {
      setActionId(null);
    }
  };

  const refreshLabel = lastRefreshedAt
    ? `Actualizar listado · ${formatRefreshTime(lastRefreshedAt)}`
    : "Actualizar listado";

  return (
    <div className={s.wrap}>
      <div className={s.headerRow}>
        <div>
          <h1 className={s.title}>Lotes (administración)</h1>
          <div className={s.subtitle}>Estado administrativo de etiquetas registradas</div>
        </div>
        <Button appearance="primary" onClick={() => void load()} disabled={refreshing || !!actionId}>
          {refreshing ? "Actualizando…" : refreshLabel}
        </Button>
      </div>

      <AppCard>
        <h2 className={s.sectionTitle}>Filtros</h2>
        <div className={s.filterRow}>
          <Text weight="semibold">Estado administrativo</Text>
          <Dropdown
            placeholder="Selecciona un filtro"
            value={
              filter === "ACTIVE"
                ? "ACTIVO"
                : filter === "INACTIVE"
                  ? "INACTIVO"
                  : filter === "BAJA"
                    ? "BAJA"
                    : ""
            }
            selectedOptions={filter ? [filter] : []}
            onOptionSelect={(_, data) => {
              setFilter(data.optionValue === "ALL" ? "" : (data.optionValue ?? ""));
            }}
          >
            <Option value="ALL">Todos</Option>
            <Option value="ACTIVE">ACTIVO</Option>
            <Option value="INACTIVE">INACTIVO</Option>
            <Option value="BAJA">BAJA</Option>
          </Dropdown>
        </div>
        <div className={s.filterHint}>
          Los lotes en baja o inactivos no aparecen en la consulta operativa estándar ni permiten
          impresión ni cambio de estatus dinámico.
        </div>
      </AppCard>

      <AppCard>
        <h2 className={s.sectionTitle}>Listado de lotes</h2>
        {items === null ? (
          <Text>Cargando…</Text>
        ) : items.length === 0 ? (
          <Text className={s.muted}>No hay lotes para este filtro.</Text>
        ) : (
          <div style={{ overflowX: "auto" }}>
            <Table aria-label="Lotes">
              <TableHeader>
                <TableRow>
                  <TableHeaderCell>Lote</TableHeaderCell>
                  <TableHeaderCell>Código</TableHeaderCell>
                  <TableHeaderCell>Nombre</TableHeaderCell>
                  <TableHeaderCell>Estado</TableHeaderCell>
                  <TableHeaderCell>Alta en sistema</TableHeaderCell>
                  <TableHeaderCell>Acciones</TableHeaderCell>
                </TableRow>
              </TableHeader>
              <TableBody>
                {items.map((row) => {
                  const busy = actionId === row.id;
                  return (
                    <TableRow key={row.id} className="table-hover-row">
                      <TableCell>{row.lote}</TableCell>
                      <TableCell>{row.codigo}</TableCell>
                      <TableCell>{row.nombre}</TableCell>
                      <TableCell>{row.adminStatusDisplay}</TableCell>
                      <TableCell>
                        {row.createdAt ? new Date(row.createdAt).toLocaleString("es-MX") : "—"}
                      </TableCell>
                      <TableCell>
                        <div className={s.actions}>
                          {row.adminStatus === "ACTIVE" ? (
                            <>
                              <Button
                                appearance="transparent"
                                className={s.linkBtn}
                                disabled={busy || refreshing}
                                onClick={() => void setAdminStatus(row.id, "INACTIVE")}
                              >
                                {busy ? "…" : "Marcar inactivo"}
                              </Button>
                              <Button
                                appearance="transparent"
                                className={s.danger}
                                disabled={busy || refreshing}
                                onClick={() => void setAdminStatus(row.id, "BAJA")}
                              >
                                Dar de baja
                              </Button>
                            </>
                          ) : (
                            <>
                              <Button
                                appearance="transparent"
                                className={s.linkBtn}
                                disabled={busy || refreshing}
                                onClick={() => void setAdminStatus(row.id, "ACTIVE")}
                              >
                                {busy ? "…" : "Reactivar"}
                              </Button>
                              {row.adminStatus === "INACTIVE" ? (
                                <Button
                                  appearance="transparent"
                                  className={s.danger}
                                  disabled={busy || refreshing}
                                  onClick={() => void setAdminStatus(row.id, "BAJA")}
                                >
                                  Dar de baja
                                </Button>
                              ) : null}
                            </>
                          )}
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
