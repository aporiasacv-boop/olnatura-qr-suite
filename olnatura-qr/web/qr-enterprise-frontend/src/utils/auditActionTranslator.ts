/**
 * Traductor centralizado de acciones de auditoría y roles para presentación en la UI web.
 * Debe mantenerse alineado con AuditActionTranslator.java del backend.
 */

const ACTION_LABELS: Record<string, string> = {
  PRINT_LABEL: "Impresión de etiquetas",
  GENERATE_LABEL: "Generación de etiquetas",
  SCAN_QR: "Escaneo de código QR",
  SCAN: "Escaneo de código QR",
  LOGIN_SUCCESS: "Inicio de sesión",
  LOGOUT: "Cierre de sesión",
  EXPORT_AUDIT_PDF: "Exportación de auditoría (PDF)",
  EXPORT_AUDIT_CSV: "Exportación de auditoría (CSV)",
  EXPORT_EXECUTIVE_DASHBOARD: "Exportación de dashboard ejecutivo",
  ADD_LOTE_COMMENT: "Comentario agregado al lote",
  ADMIN_CORRECT_LABEL: "Corrección administrativa",
  ADMIN_CORRECT_STATUS: "Corrección administrativa de estado",
  CHANGE_STATUS: "Cambio de estado",
  CHANGE_LOT_ADMIN_STATUS: "Cambio de estado administrativo del lote",
  APPROVE_USER: "Aprobación de usuario",
  REJECT_USER: "Rechazo de usuario",
  ACCESS_REQUEST: "Solicitud de acceso",
  DOWNLOAD_LABEL: "Descarga de etiqueta",
  APPROVE_MATERIAL: "Aprobación de material",
  REJECT_MATERIAL: "Rechazo de material",
  UPDATE_USER: "Actualización de usuario",
};

const ROLE_LABELS: Record<string, string> = {
  ADMIN: "Administrador",
  CALIDAD: "Calidad",
  INSPECCION: "Inspección",
  ALMACEN: "Almacén",
  PRODUCCION: "Producción",
};

const UUID_PATTERN =
  /^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/;

export function translateAuditAction(actionType: string | null | undefined): string {
  if (!actionType) return "—";
  const key = actionType.trim().toUpperCase();
  return ACTION_LABELS[key] ?? actionType.trim();
}

export function translateRole(role: string | null | undefined): string {
  if (!role) return "—";
  const key = role.trim().toUpperCase();
  return ROLE_LABELS[key] ?? role.trim();
}

export function formatUsernameForDisplay(username: string | null | undefined): string {
  if (!username || !username.trim()) return "—";
  const trimmed = username.trim();
  if (UUID_PATTERN.test(trimmed)) return "—";
  if (trimmed.includes(".") && !trimmed.includes("@")) {
    const parts = trimmed
      .split(".")
      .filter(Boolean)
      .map((p) => p.charAt(0).toUpperCase() + p.slice(1).toLowerCase());
    if (parts.length > 0) return parts.join(" ");
  }
  return trimmed;
}

export function resolveUserDisplay(
  actorDisplay?: string | null,
  username?: string | null,
  actorEmail?: string | null,
  scannedBy?: string | null
): string {
  if (actorDisplay && actorDisplay.trim() && actorDisplay !== "—") {
    return actorDisplay.trim();
  }
  if (username && username.trim()) {
    const formatted = formatUsernameForDisplay(username);
    if (formatted !== "—") return formatted;
  }
  if (actorEmail && actorEmail.trim() && !UUID_PATTERN.test(actorEmail.trim())) {
    return actorEmail.trim();
  }
  if (scannedBy && !UUID_PATTERN.test(String(scannedBy).trim())) {
    return formatUsernameForDisplay(String(scannedBy));
  }
  return "—";
}

export const AUDIT_ACTION_FILTER_OPTIONS = [
  { value: "", label: "Todas" },
  ...Object.entries(ACTION_LABELS).map(([value, label]) => ({ value, label })),
];

export function allAuditActionTranslations(): Record<string, string> {
  return { ...ACTION_LABELS };
}

export function allRoleTranslations(): Record<string, string> {
  return { ...ROLE_LABELS };
}
