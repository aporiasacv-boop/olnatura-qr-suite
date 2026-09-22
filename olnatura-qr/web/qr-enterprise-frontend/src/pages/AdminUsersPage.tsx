import * as React from "react";
import {
  Button,
  Dialog,
  DialogActions,
  DialogBody,
  DialogContent,
  DialogSurface,
  DialogTitle,
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
import PasswordField from "../components/ui/PasswordField";
import { brand } from "../styles/brand";
import { displayUserIdentity } from "../utils/auditActionTranslator";
import { downloadUsersPdf } from "../utils/downloadUsersPdf";
import {
  isValidPassword,
  passwordChecks,
  PASSWORD_RULE_LABELS,
} from "../utils/credentialRules";
import {
  EMAIL_CELL,
  TABLE_DATA_CLASS,
  TABLE_FIXED_STYLE,
  TABLE_SCROLL_WRAP,
  TRUNCATE_CELL,
  WRAP_CELL,
  cellTitle,
} from "../utils/tablePresentation";

type UserAdmin = {
  id: string;
  username: string;
  email: string;
  role: string;
  estado: string;
  enabled: boolean;
  canCreateLoteComments?: boolean;
  createdAt?: string;
};

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
  actions: { display: "flex", flexDirection: "column", alignItems: "flex-start", gap: "6px" },
  danger: { color: brand.dangerFg, fontWeight: 600, minHeight: "auto", padding: "0 4px" },
  headerActions: { display: "flex", gap: "8px", flexWrap: "wrap", alignItems: "center" },
  hint: { fontSize: "13px", color: brand.muted, margin: "4px 0 0", lineHeight: 1.4 },
  dialogForm: { display: "grid", gap: "10px" },
  rules: { display: "grid", gap: "2px", fontSize: "12px" },
  ruleOk: { color: "#1B5E35" },
  rulePending: { color: brand.muted },
  fieldErr: { color: brand.dangerFg, fontSize: "12px" },
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
  const [exportingPdf, setExportingPdf] = React.useState(false);
  const [actionId, setActionId] = React.useState<string | null>(null);
  const [lastRefreshedAt, setLastRefreshedAt] = React.useState<Date | null>(null);
  const [resetUser, setResetUser] = React.useState<UserAdmin | null>(null);
  const [newPassword, setNewPassword] = React.useState("");
  const [confirmPassword, setConfirmPassword] = React.useState("");
  const [resetBusy, setResetBusy] = React.useState(false);

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

  const patchUser = async (
    id: string,
    body: { enabled?: boolean; role?: string; canCreateLoteComments?: boolean }
  ) => {
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

  const closeReset = () => {
    if (resetBusy) return;
    setResetUser(null);
    setNewPassword("");
    setConfirmPassword("");
  };

  const submitReset = async () => {
    if (!resetUser || resetBusy) return;
    if (!isValidPassword(newPassword)) {
      toasts.push({
        intent: "error",
        title: "Contraseña inválida",
        message: "Debe tener mínimo 8 caracteres, mayúscula, minúscula y número.",
      });
      return;
    }
    if (newPassword !== confirmPassword) {
      toasts.push({
        intent: "error",
        title: "No coinciden",
        message: "La confirmación no es igual a la contraseña.",
      });
      return;
    }
    setResetBusy(true);
    try {
      await api<void>(`/admin/users/${resetUser.id}/reset-password`, {
        method: "POST",
        body: { password: newPassword },
        toast: false,
      });
      toasts.push({
        intent: "success",
        title: "Contraseña restablecida",
        message: `Entrégasela a ${displayUserIdentity(undefined, resetUser.username)}. No hace falta dar de alta otra vez.`,
      });
      setResetUser(null);
      setNewPassword("");
      setConfirmPassword("");
    } catch (err) {
      const ae = err as ApiError;
      toasts.push({
        intent: "error",
        title: "No se pudo restablecer",
        message: ae?.message ?? "Intenta de nuevo.",
        error: ae,
      });
    } finally {
      setResetBusy(false);
    }
  };

  const refreshLabel = lastRefreshedAt
    ? `Actualizar · ${formatRefreshTime(lastRefreshedAt)}`
    : "Actualizar";

  const exportPdf = async () => {
    if (exportingPdf || refreshing || !!actionId) return;
    setExportingPdf(true);
    try {
      const filename = await downloadUsersPdf((msg) => {
        toasts.push({
          intent: "error",
          title: "No se pudo generar el PDF",
          message: msg,
        });
      });
      if (filename) {
        toasts.push({
          intent: "success",
          title: "PDF listo",
          message: `${filename} — preparado para imprimir.`,
        });
      }
    } finally {
      setExportingPdf(false);
    }
  };

  return (
    <div className={s.wrap}>
      <AppCard>
        <div className={s.headerRow}>
          <div>
            <h1 className={s.title}>Usuarios</h1>
            <p className={s.hint}>
              Si alguien olvida la contraseña, restablécela aquí. No deshabilites ni borres lotes.
            </p>
          </div>
          <div className={s.headerActions}>
            <Button
              appearance="secondary"
              onClick={() => void exportPdf()}
              disabled={exportingPdf || refreshing || !!actionId || items === null || items.length === 0}
            >
              {exportingPdf ? "Generando PDF…" : "Descargar PDF para imprimir"}
            </Button>
            <Button appearance="primary" onClick={() => void load()} disabled={refreshing || !!actionId || exportingPdf}>
              {refreshing ? "Actualizando…" : refreshLabel}
            </Button>
          </div>
        </div>

        {items === null ? (
          <Text>Cargando…</Text>
        ) : items.length === 0 ? (
          <Text className={s.muted}>No hay usuarios registrados.</Text>
        ) : (
          <div style={TABLE_SCROLL_WRAP}>
            <Table
              aria-label="Usuarios"
              className={TABLE_DATA_CLASS}
              style={TABLE_FIXED_STYLE}
            >
              <TableHeader>
                <TableRow>
                  <TableHeaderCell style={{ width: "16%" }}>Usuario</TableHeaderCell>
                  <TableHeaderCell style={{ width: "22%" }}>Correo</TableHeaderCell>
                  <TableHeaderCell style={{ width: "16%" }}>Rol</TableHeaderCell>
                  <TableHeaderCell style={{ width: "10%" }}>Estado</TableHeaderCell>
                  <TableHeaderCell style={{ width: "10%" }}>Habilitado</TableHeaderCell>
                  <TableHeaderCell style={{ width: "14%" }}>Comentarios</TableHeaderCell>
                  <TableHeaderCell style={{ width: "12%" }}>Acciones</TableHeaderCell>
                </TableRow>
              </TableHeader>
              <TableBody>
                {items.map((u) => {
                  const isSelf = me?.id != null && String(me.id) === String(u.id);
                  const rowBusy = actionId === u.id;
                  const usuario = displayUserIdentity(undefined, u.username);
                  const canComment = !!u.canCreateLoteComments;
                  return (
                    <TableRow key={u.id} className="table-hover-row">
                      <TableCell style={WRAP_CELL} title={cellTitle(usuario)}>
                        {usuario}
                      </TableCell>
                      <TableCell style={EMAIL_CELL} title={cellTitle(u.email)}>
                        {u.email}
                      </TableCell>
                      <TableCell>
                        <Dropdown
                          style={{ minWidth: 0, width: "100%", maxWidth: "100%" }}
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
                          <Option value="VALIDACION">VALIDACIÓN</Option>
                        </Dropdown>
                      </TableCell>
                      <TableCell style={TRUNCATE_CELL} title={cellTitle(u.estado)}>
                        {u.estado}
                      </TableCell>
                      <TableCell>{u.enabled ? "Sí" : "No"}</TableCell>
                      <TableCell>
                        <Button
                          appearance="transparent"
                          disabled={rowBusy || refreshing || !u.enabled}
                          onClick={() =>
                            void patchUser(u.id, { canCreateLoteComments: !canComment })
                          }
                        >
                          {rowBusy ? "…" : canComment ? "Puede crear" : "Solo ver"}
                        </Button>
                      </TableCell>
                      <TableCell>
                        <div className={s.actions}>
                          <Button
                            appearance="transparent"
                            disabled={rowBusy || refreshing}
                            onClick={() => {
                              setResetUser(u);
                              setNewPassword("");
                              setConfirmPassword("");
                            }}
                          >
                            Restablecer contraseña
                          </Button>
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

      <Dialog
        open={resetUser != null}
        onOpenChange={(_, data) => {
          if (!data.open) closeReset();
        }}
      >
        <DialogSurface>
          <DialogBody>
            <DialogTitle>
              Restablecer contraseña
              {resetUser ? ` · ${displayUserIdentity(undefined, resetUser.username)}` : ""}
            </DialogTitle>
            <DialogContent>
              <div className={s.dialogForm}>
                <Text>
                  El usuario sigue siendo el mismo. Solo cambia la contraseña. Los lotes no se tocan.
                </Text>
                {resetUser && !resetUser.enabled ? (
                  <Text>
                    Esta cuenta está deshabilitada. Después de guardar, pulsa Habilitar para que pueda entrar.
                  </Text>
                ) : null}
                <PasswordField
                  size="medium"
                  value={newPassword}
                  onChange={setNewPassword}
                  placeholder="Nueva contraseña"
                  autoComplete="new-password"
                  disabled={resetBusy}
                />
                <PasswordField
                  size="medium"
                  value={confirmPassword}
                  onChange={setConfirmPassword}
                  placeholder="Confirmar contraseña"
                  autoComplete="new-password"
                  disabled={resetBusy}
                />
                <div className={s.rules}>
                  {PASSWORD_RULE_LABELS.map(({ key, label }) => {
                    const ok = passwordChecks(newPassword)[key];
                    return (
                      <span key={key} className={ok ? s.ruleOk : s.rulePending}>
                        {ok ? "✓" : "○"} {label}
                      </span>
                    );
                  })}
                </div>
                {confirmPassword && newPassword !== confirmPassword ? (
                  <span className={s.fieldErr}>Las contraseñas no coinciden.</span>
                ) : null}
              </div>
            </DialogContent>
            <DialogActions>
              <Button appearance="secondary" onClick={closeReset} disabled={resetBusy}>
                Cancelar
              </Button>
              <Button
                appearance="primary"
                onClick={() => void submitReset()}
                disabled={
                  resetBusy ||
                  !isValidPassword(newPassword) ||
                  newPassword !== confirmPassword
                }
              >
                {resetBusy ? "Guardando…" : "Guardar contraseña"}
              </Button>
            </DialogActions>
          </DialogBody>
        </DialogSurface>
      </Dialog>
    </div>
  );
}
