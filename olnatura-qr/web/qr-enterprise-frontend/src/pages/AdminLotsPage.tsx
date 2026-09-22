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
import {
  TABLE_DATA_CLASS,
  TABLE_FIXED_STYLE,
  TABLE_SCROLL_WRAP,
  TRUNCATE_CELL,
  TRUNCATE_CELL_PRIORITY,
  WRAP_CELL,
  cellTitle,
} from "../utils/tablePresentation";

type LotAdmin = {
  id: string;
  lote: string;
  codigo: string;
  nombre: string;
  adminStatus: string;
  adminStatusDisplay: string;
  workflowStatus?: string;
  createdAt?: string;
  reprintRequired?: boolean;
  reprintRequiredAt?: string | null;
};

type AlignChange = {
  field?: string;
  fieldLabel?: string;
  from?: string;
  to?: string;
};

type AlignRow = {
  labelId: string;
  lote: string;
  outcome: string;
  message: string;
  changes: AlignChange[];
  reprintRequired?: boolean;
};

type AlignBatchSummary = {
  total: number;
  offset: number;
  limit: number;
  processed: number;
  nextOffset: number;
  done: boolean;
  updated: number;
  unchanged: number;
  notFoundInDynamics: number;
  failed: number;
  rows: AlignRow[];
};

const BATCH_SIZE = 20;

const useStyles = makeStyles({
  wrap: { display: "grid", gap: "16px", minWidth: 0, maxWidth: "100%" },
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
  filterRow: { display: "grid", gap: "8px", maxWidth: "320px" },
  muted: { color: brand.muted },
  actions: {
    display: "flex",
    flexDirection: "column",
    alignItems: "flex-start",
    gap: "6px",
    minWidth: 0,
    maxWidth: "100%",
  },
  danger: { color: brand.dangerFg, fontWeight: 600, minHeight: "auto", padding: "0 4px" },
  linkBtn: {
    minHeight: "auto",
    padding: "0 4px",
    fontWeight: 600,
    whiteSpace: "normal",
    textAlign: "left",
    height: "auto",
  },
  progress: {
    fontSize: "14px",
    color: brand.text2,
    marginBottom: "10px",
    lineHeight: 1.45,
  },
  changeLine: {
    fontSize: "12px",
    color: brand.muted,
    display: "block",
  },
  outcomeUpdated: { color: brand.warningFg, fontWeight: 600 },
  outcomeNotFound: { color: brand.dangerFg, fontWeight: 600 },
  outcomeFailed: { color: brand.dangerFg, fontWeight: 600 },
  outcomeOk: { color: brand.successFg, fontWeight: 600 },
  reprintBadge: {
    color: brand.warningFg,
    fontWeight: 700,
    fontSize: "12px",
  },
});

function formatRefreshTime(d: Date | null): string {
  if (!d) return "";
  return d.toLocaleTimeString("es-MX", { hour: "2-digit", minute: "2-digit", second: "2-digit" });
}

function outcomeLabel(outcome: string): string {
  switch (outcome) {
    case "UPDATED":
      return "Actualizado";
    case "UNCHANGED":
      return "Sin cambios";
    case "NOT_FOUND":
      return "No en Dynamics";
    case "FAILED":
      return "Error";
    case "CONFIRMED":
      return "Reimpresión confirmada";
    default:
      return outcome || "—";
  }
}

function formatChanges(changes: AlignChange[] | undefined): string {
  if (!changes || changes.length === 0) return "";
  return changes
    .map((c) => `${c.fieldLabel ?? c.field ?? "?"}: ${c.from ?? "—"} → ${c.to ?? "—"}`)
    .join(" · ");
}

export default function AdminLotsPage() {
  const s = useStyles();
  const toasts = useToasts();
  const [filter, setFilter] = React.useState<string>("");
  const [items, setItems] = React.useState<LotAdmin[] | null>(null);
  const [refreshing, setRefreshing] = React.useState(false);
  const [actionId, setActionId] = React.useState<string | null>(null);
  const [lastRefreshedAt, setLastRefreshedAt] = React.useState<Date | null>(null);

  const [syncBusy, setSyncBusy] = React.useState(false);
  const [syncProgress, setSyncProgress] = React.useState<string | null>(null);
  const [syncRows, setSyncRows] = React.useState<AlignRow[]>([]);
  const [syncTotals, setSyncTotals] = React.useState<{
    total: number;
    updated: number;
    unchanged: number;
    notFoundInDynamics: number;
    failed: number;
  } | null>(null);

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

  const deleteLot = async (id: string, lote: string) => {
    if (actionId || syncBusy) return;
    const first = window.confirm(
      `¿Eliminar el lote ${lote}?\n\nSe borra el registro operativo (etiqueta, QR, comentarios y escaneos).\nEl número de lote quedará libre para registrarlo de nuevo.\nINACTIVO/BAJA no se usan: esto sí libera el lote.\nEsta acción no se puede deshacer.`
    );
    if (!first) return;
    const second = window.confirm(
      `Confirmación final.\n\n¿Seguro que quieres eliminar el lote ${lote} ahora?`
    );
    if (!second) return;
    setActionId(id);
    try {
      await api(`/admin/lots/${id}/delete`, {
        method: "POST",
        body: { confirm: "ELIMINAR_LOTE", lote },
        toast: false,
      });
      setItems((prev) => (prev ?? []).filter((x) => x.id !== id));
      setLastRefreshedAt(new Date());
      toasts.push({
        intent: "success",
        title: "Lote eliminado",
        message: `${lote} quedó libre para un nuevo registro.`,
      });
    } catch (err) {
      const ae = err as ApiError;
      toasts.push({
        intent: "error",
        title: "No se pudo eliminar el lote",
        message: ae?.message ?? "Intenta de nuevo.",
        error: ae,
      });
    } finally {
      setActionId(null);
    }
  };

  const syncAllWithDynamics = async () => {
    if (syncBusy || actionId) return;
    const ok = window.confirm(
      "¿Sincronizar todos los lotes con Dynamics?\n\nSe procesan de " +
        BATCH_SIZE +
        " en " +
        BATCH_SIZE +
        ".\nSe traen nombre, código, fechas y estado operativo actuales.\nSe conservan envases, cantidad por envase y token QR."
    );
    if (!ok) return;

    setSyncBusy(true);
    setSyncRows([]);
    setSyncTotals(null);
    setSyncProgress("Iniciando…");

    let offset = 0;
    let total = 0;
    let updated = 0;
    let unchanged = 0;
    let notFoundInDynamics = 0;
    let failed = 0;
    const allRows: AlignRow[] = [];

    try {
      while (true) {
        const batch = await api<AlignBatchSummary>(
          `/admin/lots/sync-dynamics-batch?offset=${offset}&limit=${BATCH_SIZE}`,
          { method: "POST", toast: false }
        );
        total = batch.total;
        updated += batch.updated;
        unchanged += batch.unchanged;
        notFoundInDynamics += batch.notFoundInDynamics;
        failed += batch.failed;
        if (Array.isArray(batch.rows)) {
          allRows.push(...batch.rows);
        }
        setSyncRows([...allRows]);
        setSyncTotals({ total, updated, unchanged, notFoundInDynamics, failed });
        const doneCount = Math.min(batch.nextOffset, batch.total);
        setSyncProgress(`Procesados ${doneCount} de ${batch.total}…`);
        if (batch.done) break;
        offset = batch.nextOffset;
      }

      toasts.push({
        intent: "success",
        title: "Sincronización con Dynamics",
        message: `Total ${total}: ${updated} actualizados, ${unchanged} sin cambios, ${notFoundInDynamics} no encontrados, ${failed} fallidos.`,
      });
      setSyncProgress(`Completado: ${total} lotes.`);
      await load();
    } catch (err) {
      const ae = err as ApiError;
      toasts.push({
        intent: "error",
        title: "No se pudo sincronizar con Dynamics",
        message: ae?.message ?? "Intenta de nuevo.",
        error: ae,
      });
      setSyncProgress("Interrumpido por error.");
    } finally {
      setSyncBusy(false);
    }
  };

  const syncOneWithDynamics = async (id: string, lote: string) => {
    if (syncBusy || actionId) return;
    setActionId(id);
    try {
      const row = await api<AlignRow>(`/admin/lots/${id}/sync-dynamics`, {
        method: "POST",
        toast: false,
      });
      setSyncRows((prev) => {
        const without = prev.filter((r) => r.labelId !== row.labelId);
        return [row, ...without];
      });
      setSyncTotals((prev) => {
        const base = prev ?? {
          total: 1,
          updated: 0,
          unchanged: 0,
          notFoundInDynamics: 0,
          failed: 0,
        };
        return {
          ...base,
          updated: base.updated + (row.outcome === "UPDATED" ? 1 : 0),
          unchanged: base.unchanged + (row.outcome === "UNCHANGED" ? 1 : 0),
          notFoundInDynamics: base.notFoundInDynamics + (row.outcome === "NOT_FOUND" ? 1 : 0),
          failed: base.failed + (row.outcome === "FAILED" ? 1 : 0),
        };
      });
      toasts.push({
        intent: row.outcome === "FAILED" ? "error" : "success",
        title: `Sincronizar ${lote}`,
        message: outcomeLabel(row.outcome) + (row.message ? ` · ${row.message}` : ""),
      });
      if (row.outcome === "UPDATED") await load();
    } catch (err) {
      const ae = err as ApiError;
      toasts.push({
        intent: "error",
        title: "No se pudo sincronizar el lote",
        message: ae?.message ?? "Intenta de nuevo.",
        error: ae,
      });
    } finally {
      setActionId(null);
    }
  };

  const confirmReprint = async (id: string, lote: string) => {
    if (syncBusy || actionId) return;
    const ok = window.confirm(
      `¿Confirmar que ya reimprimiste y reemplazaste las etiquetas físicas del lote ${lote}?`
    );
    if (!ok) return;
    setActionId(id);
    try {
      const row = await api<AlignRow>(`/admin/lots/${id}/confirm-reprint`, {
        method: "POST",
        toast: false,
      });
      setItems((prev) =>
        (prev ?? []).map((x) =>
          x.id === id ? { ...x, reprintRequired: false, reprintRequiredAt: null } : x
        )
      );
      toasts.push({
        intent: "success",
        title: `Reimpresión · ${lote}`,
        message: row.message || "Confirmada",
      });
    } catch (err) {
      const ae = err as ApiError;
      toasts.push({
        intent: "error",
        title: "No se pudo confirmar la reimpresión",
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

  const exposedRows = syncRows.filter(
    (r) =>
      r.outcome === "UPDATED" ||
      r.outcome === "NOT_FOUND" ||
      r.outcome === "FAILED" ||
      !!r.reprintRequired
  );

  const pendingReprintCount = (items ?? []).filter((x) => x.reprintRequired).length;

  return (
    <div className={s.wrap}>
      <div className={s.headerRow}>
        <div>
          <h1 className={s.title}>Lotes (administración)</h1>
          <p className={s.subtitle}>
            Trae a la base los datos actuales de Dynamics (nombre, código, fechas y estado).
            Se conservan envases, cantidad por envase y token QR.
            {pendingReprintCount > 0
              ? ` · ${pendingReprintCount} lote(s) con pendiente de reimpresión (correcciones manuales).`
              : ""}
          </p>
        </div>
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
          <Button
            appearance="primary"
            onClick={() => void syncAllWithDynamics()}
            disabled={refreshing || syncBusy || !!actionId}
          >
            {syncBusy ? "Sincronizando…" : "Sincronizar lotes con Dynamics"}
          </Button>
          <Button appearance="secondary" onClick={() => void load()} disabled={refreshing || syncBusy || !!actionId}>
            {refreshing ? "Actualizando…" : refreshLabel}
          </Button>
        </div>
      </div>

      {(syncProgress || syncTotals) && (
        <AppCard>
          <h2 className={s.sectionTitle}>Resultado de sincronización</h2>
          {syncProgress ? <Text className={s.progress}>{syncProgress}</Text> : null}
          {syncTotals ? (
            <Text className={s.progress}>
              Total {syncTotals.total}: {syncTotals.updated} actualizados,{" "}
              {syncTotals.unchanged} sin cambios, {syncTotals.notFoundInDynamics} no en Dynamics,{" "}
              {syncTotals.failed} con error.
            </Text>
          ) : null}
          {exposedRows.length === 0 ? (
            <Text className={s.muted}>
              {syncBusy
                ? "Esperando resultados del lote actual…"
                : "Todos los lotes procesados coinciden con Dynamics o no hubo cambios que reportar."}
            </Text>
          ) : (
            <div style={TABLE_SCROLL_WRAP}>
              <Table
                aria-label="Resultado de sincronización con Dynamics"
                className={TABLE_DATA_CLASS}
                style={TABLE_FIXED_STYLE}
              >
                <TableHeader>
                  <TableRow>
                    <TableHeaderCell style={{ width: "18%" }}>Lote</TableHeaderCell>
                    <TableHeaderCell style={{ width: "16%" }}>Resultado</TableHeaderCell>
                    <TableHeaderCell style={{ width: "66%" }}>Detalle</TableHeaderCell>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {exposedRows.map((row) => {
                    const outcomeClass =
                      row.outcome === "UPDATED"
                        ? s.outcomeUpdated
                        : row.outcome === "NOT_FOUND" || row.outcome === "FAILED"
                          ? s.outcomeFailed
                          : s.outcomeOk;
                    const detail = formatChanges(row.changes) || row.message || "—";
                    return (
                      <TableRow key={`${row.labelId}-${row.outcome}`} className="table-hover-row">
                        <TableCell style={TRUNCATE_CELL_PRIORITY} title={cellTitle(row.lote)}>
                          {row.lote || "—"}
                        </TableCell>
                        <TableCell>
                          <span className={outcomeClass}>{outcomeLabel(row.outcome)}</span>
                        </TableCell>
                        <TableCell style={WRAP_CELL} title={cellTitle(detail)}>
                          {detail}
                          {row.reprintRequired ? (
                            <span className={s.changeLine}>
                              Pendiente reimprimir y reemplazar etiqueta física.
                            </span>
                          ) : null}
                          {row.changes && row.changes.length > 0 ? (
                            <span className={s.changeLine}>{row.message}</span>
                          ) : null}
                        </TableCell>
                      </TableRow>
                    );
                  })}
                </TableBody>
              </Table>
            </div>
          )}
        </AppCard>
      )}

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
      </AppCard>

      <AppCard>
        <h2 className={s.sectionTitle}>Listado de lotes</h2>
        {items === null ? (
          <Text>Cargando…</Text>
        ) : items.length === 0 ? (
          <Text className={s.muted}>No hay lotes para este filtro.</Text>
        ) : (
          <div style={TABLE_SCROLL_WRAP}>
            <Table
              aria-label="Lotes"
              className={TABLE_DATA_CLASS}
              style={TABLE_FIXED_STYLE}
            >
              <TableHeader>
                <TableRow>
                  <TableHeaderCell style={{ width: "16%" }}>Lote</TableHeaderCell>
                  <TableHeaderCell style={{ width: "10%" }}>Código</TableHeaderCell>
                  <TableHeaderCell style={{ width: "14%" }}>Nombre</TableHeaderCell>
                  <TableHeaderCell style={{ width: "12%" }}>Estado administrativo</TableHeaderCell>
                  <TableHeaderCell style={{ width: "12%" }}>Etiqueta física</TableHeaderCell>
                  <TableHeaderCell style={{ width: "12%" }}>Alta en sistema</TableHeaderCell>
                  <TableHeaderCell style={{ width: "20%" }}>Acciones</TableHeaderCell>
                </TableRow>
              </TableHeader>
              <TableBody>
                {items.map((row) => {
                  const busy = actionId === row.id;
                  const alta = row.createdAt
                    ? new Date(row.createdAt).toLocaleString("es-MX")
                    : "—";
                  return (
                    <TableRow key={row.id} className="table-hover-row">
                      <TableCell style={TRUNCATE_CELL_PRIORITY} title={cellTitle(row.lote)}>
                        {row.lote}
                      </TableCell>
                      <TableCell style={TRUNCATE_CELL} title={cellTitle(row.codigo)}>
                        {row.codigo}
                      </TableCell>
                      <TableCell style={WRAP_CELL} title={cellTitle(row.nombre)}>
                        {row.nombre}
                      </TableCell>
                      <TableCell style={TRUNCATE_CELL} title={cellTitle(row.adminStatusDisplay)}>
                        {row.adminStatusDisplay}
                      </TableCell>
                      <TableCell style={WRAP_CELL}>
                        {row.reprintRequired ? (
                          <span className={s.reprintBadge}>Pendiente reimpresión</span>
                        ) : (
                          <span className={s.muted}>OK</span>
                        )}
                      </TableCell>
                      <TableCell style={TRUNCATE_CELL} title={cellTitle(alta)}>
                        {alta}
                      </TableCell>
                      <TableCell>
                        <div className={s.actions}>
                          <Button
                            appearance="transparent"
                            className={s.linkBtn}
                            disabled={busy || refreshing || syncBusy}
                            onClick={() => void syncOneWithDynamics(row.id, row.lote)}
                          >
                            {busy ? "…" : "Sincronizar con Dynamics"}
                          </Button>
                          {row.reprintRequired ? (
                            <Button
                              appearance="transparent"
                              className={s.linkBtn}
                              disabled={busy || refreshing || syncBusy}
                              onClick={() => void confirmReprint(row.id, row.lote)}
                            >
                              {busy ? "…" : "Confirmar reimpresión"}
                            </Button>
                          ) : null}
                          {row.adminStatus === "ACTIVE" ? (
                            <>
                              <Button
                                appearance="transparent"
                                className={s.linkBtn}
                                disabled={busy || refreshing || syncBusy}
                                onClick={() => void setAdminStatus(row.id, "INACTIVE")}
                              >
                                {busy ? "…" : "Marcar inactivo"}
                              </Button>
                              <Button
                                appearance="transparent"
                                className={s.danger}
                                disabled={busy || refreshing || syncBusy}
                                onClick={() => void setAdminStatus(row.id, "BAJA")}
                              >
                                Dar de baja
                              </Button>
                              <Button
                                appearance="transparent"
                                className={s.danger}
                                disabled={busy || refreshing || syncBusy}
                                onClick={() => void deleteLot(row.id, row.lote)}
                              >
                                Eliminar lote
                              </Button>
                            </>
                          ) : (
                            <>
                              <Button
                                appearance="transparent"
                                className={s.linkBtn}
                                disabled={busy || refreshing || syncBusy}
                                onClick={() => void setAdminStatus(row.id, "ACTIVE")}
                              >
                                {busy ? "…" : "Reactivar"}
                              </Button>
                              {row.adminStatus === "INACTIVE" ? (
                                <Button
                                  appearance="transparent"
                                  className={s.danger}
                                  disabled={busy || refreshing || syncBusy}
                                  onClick={() => void setAdminStatus(row.id, "BAJA")}
                                >
                                  Dar de baja
                                </Button>
                              ) : null}
                              <Button
                                appearance="transparent"
                                className={s.danger}
                                disabled={busy || refreshing || syncBusy}
                                onClick={() => void deleteLot(row.id, row.lote)}
                              >
                                Eliminar lote
                              </Button>
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
