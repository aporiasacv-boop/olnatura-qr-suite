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
import { useAuth } from "../auth/AuthContext";
import { useToasts } from "../components/ui/toasts";
import AppCard from "../components/ui/AppCard";
import { brand } from "../styles/brand";

type UserAdmin = {
  id: string;
  username: string;
  email: string;
  role: string;
  estado: string;
  enabled: boolean;
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
    marginBottom: "16px",
  },
  title: { fontSize: "20px", fontWeight: 600, color: brand.text, margin: 0 },
  subtitle: { fontSize: "14px", color: brand.muted, marginTop: "4px" },
  muted: { color: brand.muted },
  actions: { display: "flex", flexDirection: "column", alignItems: "flex-start", gap: "6px" },
  danger: { color: brand.dangerFg, fontWeight: 600, minHeight: "auto", padding: "0 4px" },
});

function formatRefreshTime(d: Date | null): string {
  if (!d) return "";
  return d.toLocaleTimeString("es-MX", { hour: "2-digit", minute: "2-digit", second: "2-digit" });
}

export default function AdminUsersPage() {
  const s = useStyles();
  const toasts = useToasts();
  const { me } = useAuth();
  const [items, setItems] = React.useState<UserAdmin[] | null>(null);
  const [refreshing, setRefreshing] = React.useState(false);
  const [actionId, setActionId] = React.useState<string | null>(null);
  const [lastRefreshedAt, setLastRefreshedAt] = React.useState<Date | null>(null);

  const load = React.useCallback(async () => {
    setRefreshing(true);
    try {
      const res = await api<UserAdmin[]>("/admin/users", { toast: false });
      setItems(Array.isArray(res) ? res : []);
      setLastRefreshedAt(new Date());
    } catch (err) {
      const ae = err as ApiError;
      toasts.push({
        intent: "error",
        title: "No se pudo cargar usuarios",
        message: ae?.message ?? "Revisa permisos.",
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

  const patchUser = async (id: string, body: { enabled?: boolean; role?: string }) => {
    if (actionId) return;
    setActionId(id);
    try {
      const updated = await api<UserAdmin>(`/admin/users/${id}`, {
        method: "PATCH",
        body,
        toast: false,
      });
      setItems((prev) => (prev ?? []).map((u) => (u.id === id ? updated : u)));
      setLastRefreshedAt(new Date());
      toasts.push({
        intent: "success",
        title: "Usuario actualizado",
        message: updated.username,
      });
    } catch (err) {
      const ae = err as ApiError;
      toasts.push({
        intent: "error",
        title: "No se pudo actualizar",
        message: ae?.message ?? "Intenta de nuevo.",
        error: ae,
      });
    } finally {
      setActionId(null);
    }
  };

  const refreshLabel = lastRefreshedAt
    ? `Actualizar · ${formatRefreshTime(lastRefreshedAt)}`
    : "Actualizar";

  return (
    <div className={s.wrap}>
      <AppCard>
        <div className={s.headerRow}>
          <div>
            <h1 className={s.title}>Usuarios</h1>
          </div>
          <Button appearance="primary" onClick={() => void load()} disabled={refreshing || !!actionId}>
            {refreshing ? "Actualizando…" : refreshLabel}
          </Button>
        </div>

        {items === null ? (
          <Text>Cargando…</Text>
        ) : items.length === 0 ? (
          <Text className={s.muted}>No hay usuarios registrados.</Text>
        ) : (
          <div style={{ overflowX: "auto" }}>
            <Table aria-label="Usuarios">
              <TableHeader>
                <TableRow>
                  <TableHeaderCell>Usuario</TableHeaderCell>
                  <TableHeaderCell>Correo</TableHeaderCell>
                  <TableHeaderCell>Rol</TableHeaderCell>
                  <TableHeaderCell>Estado</TableHeaderCell>
                  <TableHeaderCell>Habilitado</TableHeaderCell>
                  <TableHeaderCell>Acciones</TableHeaderCell>
                </TableRow>
              </TableHeader>
              <TableBody>
                {items.map((u) => {
                  const isSelf = me?.id != null && String(me.id) === String(u.id);
                  const rowBusy = actionId === u.id;
                  return (
                    <TableRow key={u.id} className="table-hover-row">
                      <TableCell>{u.username}</TableCell>
                      <TableCell>{u.email}</TableCell>
                      <TableCell style={{ minWidth: 140 }}>
                        <Dropdown
                          value={u.role}
                          selectedOptions={[u.role]}
                          disabled={rowBusy || refreshing || (isSelf && u.role === "ADMIN")}
                          onOptionSelect={(_, data) => {
                            const role = data.optionValue;
                            if (role && role !== u.role) void patchUser(u.id, { role });
                          }}
                        >
                          <Option value="ADMIN">ADMIN</Option>
                          <Option value="ALMACEN">ALMACÉN</Option>
                          <Option value="PRODUCCION">PRODUCCIÓN</Option>
                          <Option value="CALIDAD">CONTROL DE CALIDAD</Option>
                          <Option value="INSPECCION">INSPECCIÓN</Option>
                        </Dropdown>
                      </TableCell>
                      <TableCell>{u.estado}</TableCell>
                      <TableCell>{u.enabled ? "Sí" : "No"}</TableCell>
                      <TableCell>
                        <div className={s.actions}>
                          {u.enabled ? (
                            <Button
                              appearance="transparent"
                              className={s.danger}
                              disabled={isSelf || rowBusy || refreshing}
                              onClick={() => void patchUser(u.id, { enabled: false })}
                            >
                              {rowBusy ? "…" : "Deshabilitar"}
                            </Button>
                          ) : (
                            <Button
                              appearance="transparent"
                              disabled={rowBusy || refreshing}
                              onClick={() => void patchUser(u.id, { enabled: true })}
                            >
                              {rowBusy ? "…" : "Habilitar"}
                            </Button>
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
