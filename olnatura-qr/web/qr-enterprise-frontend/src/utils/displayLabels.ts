/**
 * Frontend-only display label mappings.
 * Maps internal/technical keys to business-friendly Spanish labels.
 * Does NOT change backend fields, DTO keys, or API contracts.
 */

export const LABELS = {
  // Section headers
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

  // Actions
  downloadZpl: "Descargar etiqueta Zebra",
  downloadAuditPdf: "Descargar historial (PDF)",
  registerScan: "Registrar escaneo",

  // Empty / no data
  noData: "Sin dato",
  noRecords: "No hay registros disponibles",
  noScans: "Sin escaneos",
  noEvents: "Sin eventos",
  readyToLookup: "Listo para consultar",
  readyToFilter: "Listo para filtrar",

  // Scan history table columns
  fecha: "Fecha",
  hora: "Hora",
  usuario: "Usuario",
  dispositivo: "Dispositivo",
  accion: "Acción",
  detalle: "Detalle",
} as const;

/** Map fuente (source) technical value to friendly Spanish label */
export function fuenteDisplay(fuente: string | null | undefined): string {
  if (!fuente || typeof fuente !== "string") return LABELS.noData;
  const v = fuente.trim().toUpperCase();
  if (v === "MOCK_DYNAMICS") return "Datos demo para pruebas";
  if (v === "DB_ONLY") return "Base de datos local (sin Dynamics)";
  return fuente;
}

/** Map audit actionType (backend key) to friendly Spanish label */
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
    ACCESS_REQUEST: "Solicitud de acceso",
    DOWNLOAD_LABEL: "Descarga de etiqueta",
  };
  return map[v] ?? actionType;
}

/** Format ISO date string for display: DD/MM/YYYY HH:mm */
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
