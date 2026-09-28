import * as React from "react";
import {
  Button,
  Dialog,
  DialogActions,
  DialogBody,
  DialogContent,
  DialogSurface,
  DialogTitle,
  Input,
  makeStyles,
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
  WRAP_CELL,
  cellTitle,
} from "../utils/tablePresentation";

type LotRow = {
  id: string;
  lote: string;
  codigo: string;
  nombre: string;
  adminStatusDisplay: string;
  createdAt?: string | null;
};

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
  muted: { color: brand.muted },
  danger: { color: brand.dangerFg, fontWeight: 600 },
  confirmBox: { display: "grid", gap: "8px" },
});

export default function LotesPage() {
  const s = useStyles();
  const toasts = useToasts();
  const [items, setItems] = React.useState<LotRow[] | null>(null);
  const [refreshing, setRefreshing] = React.useState(false);
  const [actionId, setActionId] = React.useState<string | null>(null);
  const [target, setTarget] = React.useState<LotRow | null>(null);
  const [step, setStep] = React.useState<1 | 2>(1);
  const [typedLote, setTypedLote] = React.useState("");

  const load = React.useCallback(async () => {
    setRefreshing(true);
    try {
      const res = await api<LotRow[]>("/lotes", { toast: false });
      setItems(Array.isArray(res) ? res : []);
    } catch (err) {
      const ae = err as ApiError;
      toasts.push({
        intent: "error",
        title: "No se pudo cargar lotes",
        message: ae?.message ?? "Intenta de nuevo.",
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

  const closeDialog = () => {
    if (actionId) return;
    setTarget(null);
    setStep(1);
    setTypedLote("");
  };

  const confirmDelete = async () => {
    if (!target || actionId) return;
    const expected = target.lote.trim();
    if (typedLote.trim() !== expected) {
      toasts.push({
        intent: "error",
        title: "El lote no coincide",
      });
      return;
    }
    setActionId(target.id);
    try {
      await api(`/lotes/${target.id}/delete`, {
        method: "POST",
        body: { confirm: "ELIMINAR_LOTE", lote: expected },
        toast: false,
      });
      setItems((prev) => (prev ?? []).filter((x) => x.id !== target.id));
      toasts.push({
        intent: "success",
        title: "Lote eliminado",
        message: expected,
      });
      setTarget(null);
      setStep(1);
      setTypedLote("");
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

  return (
    <div className={s.wrap}>
      <div className={s.headerRow}>
        <h1 className={s.title}>Lotes</h1>
        <Button appearance="secondary" onClick={() => void load()} disabled={refreshing || !!actionId}>
          {refreshing ? "Actualizando…" : "Actualizar listado"}
        </Button>
      </div>

      <AppCard>
        {items === null ? (
          <Text>Cargando…</Text>
        ) : items.length === 0 ? (
          <Text className={s.muted}>No hay lotes.</Text>
        ) : (
          <div style={TABLE_SCROLL_WRAP}>
            <Table aria-label="Lotes" className={TABLE_DATA_CLASS} style={TABLE_FIXED_STYLE}>
              <TableHeader>
                <TableRow>
                  <TableHeaderCell style={{ width: "22%" }}>Lote</TableHeaderCell>
                  <TableHeaderCell style={{ width: "18%" }}>Código</TableHeaderCell>
                  <TableHeaderCell style={{ width: "28%" }}>Nombre</TableHeaderCell>
                  <TableHeaderCell style={{ width: "18%" }}>Estado</TableHeaderCell>
                  <TableHeaderCell style={{ width: "14%" }}>Acción</TableHeaderCell>
                </TableRow>
              </TableHeader>
              <TableBody>
                {items.map((row) => (
                  <TableRow key={row.id} className="table-hover-row">
                    <TableCell style={WRAP_CELL} title={cellTitle(row.lote)}>
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
                    <TableCell>
                      <Button
                        appearance="transparent"
                        className={s.danger}
                        disabled={!!actionId || refreshing}
                        onClick={() => {
                          setTarget(row);
                          setStep(1);
                          setTypedLote("");
                        }}
                      >
                        Eliminar
                      </Button>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
        )}
      </AppCard>

      <Dialog open={!!target} onOpenChange={(_, data) => { if (!data.open) closeDialog(); }}>
        <DialogSurface>
          <DialogBody>
            <DialogTitle>Eliminar lote</DialogTitle>
            <DialogContent>
              {step === 1 ? (
                <Text>¿Eliminar el lote {target?.lote}?</Text>
              ) : (
                <div className={s.confirmBox}>
                  <Text>Escribe el lote {target?.lote}</Text>
                  <Input
                    value={typedLote}
                    onChange={(_, d) => setTypedLote(d.value)}
                    placeholder={target?.lote ?? ""}
                    disabled={!!actionId}
                  />
                </div>
              )}
            </DialogContent>
            <DialogActions>
              <Button appearance="secondary" onClick={closeDialog} disabled={!!actionId}>
                Cancelar
              </Button>
              {step === 1 ? (
                <Button appearance="primary" onClick={() => setStep(2)}>
                  Continuar
                </Button>
              ) : (
                <Button
                  appearance="primary"
                  disabled={!!actionId || typedLote.trim() !== (target?.lote ?? "").trim()}
                  onClick={() => void confirmDelete()}
                >
                  {actionId ? "Eliminando…" : "Eliminar lote"}
                </Button>
              )}
            </DialogActions>
          </DialogBody>
        </DialogSurface>
      </Dialog>
    </div>
  );
}
