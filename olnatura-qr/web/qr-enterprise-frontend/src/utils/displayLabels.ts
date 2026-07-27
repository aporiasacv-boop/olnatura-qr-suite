
import { translateAuditAction, translateRole } from "./auditActionTranslator";

export const LABELS = {
  lookup: "Consulta por lote",
  scanHistory: "Historial de escaneos",
  comments: "Comentarios",
  commentsEmpty: "Sin comentarios en este lote",
  commentsPlaceholder: "Escribe un comentario operativo…",
  commentsAdd: "Agregar comentario",
  auditLog: "Historial de auditoría",
  metrics: "Métricas operativas",
  label: "Etiqueta",
  labelData: "Datos fijos (Etiqueta)",
  dynamicStatus: "Estado",
  dynamicState: "Estado dinámico",
  statusDynamics: "Estado de Dynamics",
  qualityOrderStatus: "Estado de orden de calidad",
  passedBatchDispositionCode: "Código de disposición (aprobado)",
  batchDispositionCode: "Código de disposición de lote",
  fuente: "Fuente de datos",
  envase: "Envase",
  cantidad: "Inventario disponible",
  ubicacion: "Ubicación",
  almacen: "Almacén",
  cantidadAlmacen: "Inventario disponible",
  unidadInventario: "Unidad de inventario",
  downloadZpl: "Descargar etiqueta Zebra",
  downloadAuditPdf: "Descargar historial (PDF)",
  noData: "Sin dato",
  noRecords: "No hay registros disponibles",
  noScans: "Sin escaneos",
  noEvents: "Sin eventos",
  readyToLookup: "Listo para consultar",
  readyToFilter: "Listo para consultar",
  fecha: "Fecha",
  hora: "Hora",
  usuario: "Usuario",
  rol: "Rol",
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
  return translateAuditAction(actionType);
}

export function roleDisplay(role: string | null | undefined): string {
  return translateRole(role);
}

const METADATA_KEY_LABELS: Record<string, string> = {
  targetUserId: "ID de usuario destino",
  targetUsername: "Usuario destino",
  targetUserName: "Usuario destino",
  roleRequested: "Rol solicitado",
  exportType: "Tipo de exportación",
  count: "Cantidad",
  countEvents: "Eventos exportados",
  bytes: "Tamaño (bytes)",
  filename: "Archivo",
  labelsExported: "Etiquetas exportadas",
  scansExported: "Escaneos exportados",
  auditsExported: "Auditorías exportadas",
  usersExported: "Usuarios exportados",
  labelId: "ID de etiqueta",
  requester: "Solicitante",
  lote: "Lote",
  mode: "Modo",
  status: "Estado",
  resultingStatus: "Estado resultante",
  rol: "Rol",
  approvalRole: "Rol de aprobación",
  tipoMaterial: "Tipo de material",
  motivo: "Motivo",
  calidadApproved: "Aprobado por Calidad",
  inspeccionApproved: "Aprobado por Inspección",
  username: "Usuario",
  email: "Correo",
  userId: "ID de usuario",
  from: "Desde",
  to: "Hasta",
  deviceId: "Dispositivo",
  changes: "Campos modificados",
  commentId: "ID de comentario",
  preview: "Vista previa",
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
  if (key === "exportType" && v === "EXECUTIVE_DASHBOARD_XLSX") return "Excel Power BI";
  if (key === "roleRequested" || key === "rol" || key === "approvalRole") {
    return translateRole(v);
  }
  if (key === "calidadApproved" || key === "inspeccionApproved") {
    if (v === "TRUE") return "Sí";
    if (v === "FALSE") return "No";
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
  metadata: Record<string, unknown> | string | null | undefined
): AuditDetailEntry[] {
  const meta = parseMetadata(metadata);
  const entries: AuditDetailEntry[] = [];
  if (!meta || Object.keys(meta).length === 0) return entries;
  for (const [k, v] of Object.entries(meta)) {
    if (k === "deviceId") continue;
    if (v == null || (typeof v === "string" && !v.trim())) continue;
    if (k === "changes" && Array.isArray(v)) {
      const lines = v
        .map((row) => {
          if (!row || typeof row !== "object") return "";
          const r = row as Record<string, unknown>;
          const field = String(r.fieldLabel ?? r.field ?? "Campo");
          return `${field}: ${String(r.from ?? "—")} → ${String(r.to ?? "—")}`;
        })
        .filter(Boolean)
        .join(" | ");
      if (lines) entries.push({ label: "Campos modificados", value: lines });
      continue;
    }
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
