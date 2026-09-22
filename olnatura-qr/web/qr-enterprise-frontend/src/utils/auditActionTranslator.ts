

const ACTION_LABELS: Record<string, string> = {
  PRINT_LABEL: "Impresión de etiquetas",
  GENERATE_LABEL: "Generación de etiquetas",
  SCAN_QR: "Consulta QR",
  SCAN: "Consulta QR",
  LOGIN_SUCCESS: "Inicio de sesión",
  LOGOUT: "Cierre de sesión",
  EXPORT_AUDIT_PDF: "Exportación de auditoría (PDF)",
  EXPORT_AUDIT_CSV: "Exportación de auditoría (CSV)",
  EXPORT_USERS_PDF: "Exportación de usuarios (PDF)",
  EXPORT_EXECUTIVE_DASHBOARD: "Exportación de dashboard ejecutivo",
  ADD_LOTE_COMMENT: "Comentario agregado al lote",
  ADMIN_CORRECT_LABEL: "Corrección administrativa",
  ADMIN_CORRECT_STATUS: "Corrección administrativa de estado",
  ADMIN_ALIGN_LABELS_DYNAMICS: "Alineación masiva con Dynamics",
  ADMIN_ALIGN_LABEL_DYNAMICS: "Alineación de lote con Dynamics",
  SYNC_OPERATIONAL_STATUS_DYNAMICS: "Sincronización de estado operativo (Dynamics)",
  PURGE_OPERATIONAL_DATA_V2: "Vaciado operativo de BD",
  PURGE_LOTS: "Eliminación de lotes",
  CHANGE_STATUS: "Cambio de estado",
  CHANGE_LOT_ADMIN_STATUS: "Cambio de estado administrativo del lote",
  ELIMINAR_LOTE: "Eliminar lote",
  APPROVE_USER: "Aprobación de usuario",
  REJECT_USER: "Rechazo de usuario",
  ACCESS_REQUEST: "Solicitud de acceso",
  DOWNLOAD_LABEL: "Descarga de etiqueta",
  APPROVE_MATERIAL: "Aprobación de material",
  REJECT_MATERIAL: "Rechazo de material",
  UPDATE_USER: "Actualización de usuario",
  RESET_USER_PASSWORD: "Restablecimiento de contraseña",
};

const ROLE_LABELS: Record<string, string> = {
  ADMIN: "Administrador",
  CALIDAD: "Calidad",
  INSPECCION: "Inspección",
  ALMACEN: "Almacén",
  PRODUCCION: "Producción",
  VALIDACION: "Validación",
};

const UUID_PATTERN =
  /^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/;

export function looksLikeUuid(value: string | null | undefined): boolean {
  return !!value && UUID_PATTERN.test(value.trim());
}

function looksLikeEmail(value: string): boolean {
  return value.includes("@");
}


export function displayUserIdentity(
  actorDisplay?: string | null,
  username?: string | null
): string {
  if (username?.trim()) {
    const formatted = formatUsernameForDisplay(username);
    if (formatted !== "—") return formatted;
  }
  if (actorDisplay?.trim() && actorDisplay !== "—") {
    const d = actorDisplay.trim();
    if (!looksLikeUuid(d) && !looksLikeEmail(d)) return d;
  }
  return "—";
}

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
  if (looksLikeUuid(trimmed)) return "—";
  if (looksLikeEmail(trimmed)) return "—";
  if ((trimmed.includes(".") || trimmed.includes("_")) && !trimmed.includes("@")) {
    const parts = trimmed.split(/[._]/).filter(Boolean);
    if (parts.length > 1) {
      return parts
        .map((p) => p.charAt(0).toUpperCase() + p.slice(1).toLowerCase())
        .join(" ");
    }
  }
  return trimmed;
}

export function formatSessionDisplayName(username: string | null | undefined): string {
  if (!username || !username.trim()) return "—";
  const trimmed = username.trim();
  if (looksLikeUuid(trimmed) || looksLikeEmail(trimmed)) return trimmed;

  return trimmed
    .split(/[.\s_]+/)
    .filter(Boolean)
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1).toLowerCase())
    .join(" ");
}


export function resolveUserDisplay(
  actorDisplay?: string | null,
  username?: string | null,
  _actorEmail?: string | null,
  scannedBy?: string | null
): string {
  const fromIdentity = displayUserIdentity(actorDisplay, username);
  if (fromIdentity !== "—") return fromIdentity;
  if (scannedBy && !looksLikeUuid(String(scannedBy).trim())) {
    return formatUsernameForDisplay(String(scannedBy));
  }
  return "—";
}

export const AUDIT_ACTION_FILTER_OPTIONS = (() => {
  const seen = new Set<string>();
  const options: { value: string; label: string }[] = [{ value: "", label: "Todas" }];
  for (const [value, label] of Object.entries(ACTION_LABELS)) {
    if (seen.has(label)) continue;
    seen.add(label);
    options.push({ value, label });
  }
  return options;
})();

export function allAuditActionTranslations(): Record<string, string> {
  return { ...ACTION_LABELS };
}

export function allRoleTranslations(): Record<string, string> {
  return { ...ROLE_LABELS };
}
