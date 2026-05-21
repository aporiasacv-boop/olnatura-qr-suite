

export const LABELS = {
  lookup: "Consulta por lote",
  scanHistory: "Historial de escaneos",
  auditLog: "Historial de auditoría",
  label: "Etiqueta",
  labelData: "Datos fijos (Etiqueta)",
  dynamicStatus: "Estado",
  dynamicState: "Estado dinámico",
  fuente: "Fuente de datos",
  envase: "Envase",
  cantidad: "Cantidad",
  ubicacion: "Ubicación",
  downloadZpl: "Descargar etiqueta Zebra",
  downloadAuditPdf: "Descargar historial (PDF)",
  registerScan: "Registrar escaneo",
  noData: "Sin dato",
  noRecords: "No hay registros disponibles",
  noScans: "Sin escaneos",
  noEvents: "Sin eventos",
  readyToLookup: "Listo para consultar",
  readyToFilter: "Listo para filtrar",
  fecha: "Fecha",
  hora: "Hora",
  usuario: "Usuario",
  dispositivo: "Dispositivo",
  accion: "Acción",
  detalle: "Detalle",
} as const;


export function fuenteDisplay(fuente: string | null | undefined): string {
  if (!fuente || typeof fuente !== "string") return LABELS.noData;
  const v = fuente.trim().toUpperCase();
  if (v === "MOCK_DYNAMICS") return "Datos demo para pruebas";
  if (v === "DB_ONLY") return "Base de datos local (sin Dynamics)";
  return fuente;
}


export function actionTypeDisplay(actionType: string | null | undefined): string {
  if (!actionType) return LABELS.noData;
  const v = actionType.trim().toUpperCase();
  const map: Record<string, string> = {
    CHANGE_STATUS: "Cambio de estado",
    SCAN: "Escaneo",
    PRINT_LABEL: "Impresión etiqueta",
    GENERATE_LABEL: "Generar etiqueta",
    EXPORT_AUDIT_PDF: "Exportación de historial PDF",
    APPROVE_USER: "Aprobación de usuario",
    REJECT_USER: "Rechazo de usuario",
    ACCESS_REQUEST: "Solicitud de acceso",
    DOWNLOAD_LABEL: "Descarga de etiqueta",
  };
  return map[v] ?? actionType;
}

const METADATA_KEY_LABELS: Record<string, string> = {
  targetUserId: "ID de usuario destino",
  targetUsername: "Usuario destino",
  targetUserName: "Usuario destino",
  roleRequested: "Rol solicitado",
  exportType: "Tipo de exportación",
  count: "Cantidad",
  countEvents: "Eventos exportados",
  labelId: "ID de etiqueta",
  requester: "Solicitante",
  lote: "Lote",
  mode: "Modo",
  status: "Estado",
  username: "Usuario",
  email: "Correo",
  userId: "ID de usuario",
  from: "Desde",
  to: "Hasta",
  deviceId: "Dispositivo",
};

export function metadataKeyToLabel(key: string): string {
  return METADATA_KEY_LABELS[key] ?? key;
}

function formatMetadataValue(key: string, value: unknown): string {
  if (value == null) return LABELS.noData;
  const str = String(value).trim();
  if (!str) return LABELS.noData;
  const v = str.toUpperCase();
  if (key === "mode" && v === "ZPL_DOWNLOAD") return "Descarga ZPL";
  if (key === "exportType" && v === "PDF") return "PDF";
  if (key === "roleRequested") {
    if (v === "ALMACEN") return "Almacén";
    if (v === "INSPECCION") return "Inspección";
    if (v === "ADMIN") return "Administrador";
  }
  return str;
}

function parseMetadata(raw: unknown): Record<string, unknown> | null {
  if (raw == null) return null;
  if (typeof raw === "object" && raw !== null && !Array.isArray(raw)) {
    return raw as Record<string, unknown>;
  }
  if (typeof raw === "string") {
    try {
      const parsed = JSON.parse(raw);
      return parsed && typeof parsed === "object" && !Array.isArray(parsed) ? parsed : null;
    } catch {
      return null;
    }
  }
  return null;
}

export type AuditDetailEntry = { label: string; value: string };
export function formatAuditDetail(
  metadata: Record<string, unknown> | string | null | undefined,
  deviceId?: string | null
): AuditDetailEntry[] {
  const meta = parseMetadata(metadata);
  const entries: AuditDetailEntry[] = [];
  if (deviceId && String(deviceId).trim()) {
    entries.push({ label: "Dispositivo", value: String(deviceId).trim() });
  }
  if (!meta || Object.keys(meta).length === 0) return entries;
  for (const [k, v] of Object.entries(meta)) {
    if (v == null || (typeof v === "string" && !v.trim())) continue;
    const label = metadataKeyToLabel(k);
    const value = formatMetadataValue(k, v);
    entries.push({ label, value });
  }
  return entries;
}

export function formatDateTime(iso: string | null | undefined): { date: string; time: string } {
  if (!iso) return { date: LABELS.noData, time: LABELS.noData };
  try {
    const d = new Date(iso);
    if (isNaN(d.getTime())) return { date: LABELS.noData, time: LABELS.noData };
    const date = d.toLocaleDateString("es-ES", { day: "2-digit", month: "2-digit", year: "numeric" });
    const time = d.toLocaleTimeString("es-ES", { hour: "2-digit", minute: "2-digit", second: "2-digit", hour12: false });
    return { date, time };
  } catch {
    return { date: LABELS.noData, time: LABELS.noData };
  }
}
