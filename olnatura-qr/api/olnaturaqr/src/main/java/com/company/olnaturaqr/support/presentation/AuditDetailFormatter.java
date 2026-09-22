package com.company.olnaturaqr.support.presentation;

import com.company.olnaturaqr.support.audit.AuditService;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class AuditDetailFormatter {

    private static final DateTimeFormatter INSTANT_FMT = DateTimeFormatter
            .ofPattern("dd/MM/yyyy HH:mm:ss")
            .withZone(AuditService.ZONE);

    private static final Set<String> SKIP_KEYS = Set.of(
            "deviceId",
            "labelId",
            "userId",
            "targetUserId",
            "commentId",
            "lote"
    );

    private static final Map<String, String> KEY_LABELS = new LinkedHashMap<>();
    private static final Map<String, String> MOTIVO_LABELS = new LinkedHashMap<>();

    static {
        KEY_LABELS.put("targetUsername", "Usuario destino");
        KEY_LABELS.put("targetUserName", "Usuario destino");
        KEY_LABELS.put("roleRequested", "Rol solicitado");
        KEY_LABELS.put("exportType", "Tipo de exportación");
        KEY_LABELS.put("count", "Cantidad");
        KEY_LABELS.put("countEvents", "Eventos exportados");
        KEY_LABELS.put("bytes", "Tamaño (bytes)");
        KEY_LABELS.put("filename", "Archivo");
        KEY_LABELS.put("labelsExported", "Etiquetas exportadas");
        KEY_LABELS.put("scansExported", "Escaneos exportados");
        KEY_LABELS.put("auditsExported", "Auditorías exportadas");
        KEY_LABELS.put("usersExported", "Usuarios exportados");
        KEY_LABELS.put("requester", "Solicitante");
        KEY_LABELS.put("lote", "Lote");
        KEY_LABELS.put("mode", "Modo");
        KEY_LABELS.put("status", "Estado");
        KEY_LABELS.put("resultingStatus", "Estado resultante");
        KEY_LABELS.put("rol", "Rol");
        KEY_LABELS.put("approvalRole", "Rol de aprobación");
        KEY_LABELS.put("tipoMaterial", "Tipo de material");
        KEY_LABELS.put("motivo", "Motivo");
        KEY_LABELS.put("motivoActualizacion", "Motivo");
        KEY_LABELS.put("calidadApproved", "Aprobado por Calidad");
        KEY_LABELS.put("inspeccionApproved", "Aprobado por Inspección");
        KEY_LABELS.put("username", "Usuario");
        KEY_LABELS.put("email", "Correo");
        KEY_LABELS.put("from", "Desde");
        KEY_LABELS.put("to", "Hasta");
        KEY_LABELS.put("estadoAnterior", "Estado anterior");
        KEY_LABELS.put("estadoNuevo", "Estado nuevo");
        KEY_LABELS.put("fecha", "Fecha");
        KEY_LABELS.put("source", "Fuente");
        KEY_LABELS.put("changes", "Campos modificados");
        KEY_LABELS.put("preview", "Vista previa");
        KEY_LABELS.put("actionType", "Acción");

        MOTIVO_LABELS.put("CONSULTA_LOTE", "Consulta de lote");
        MOTIVO_LABELS.put("SYNC_DYNAMICS_MANUAL", "Sincronización manual con Dynamics");
        MOTIVO_LABELS.put("ALIGN_FROM_DYNAMICS", "Alineación con Dynamics");
        MOTIVO_LABELS.put("GENERACION_ETIQUETA", "Generación de etiqueta");
        MOTIVO_LABELS.put("BACKFILL_STARTUP", "Actualización inicial");
    }

    private AuditDetailFormatter() {
    }

    public static String format(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> en : metadata.entrySet()) {
            String key = en.getKey();
            if (key == null || SKIP_KEYS.contains(key)) continue;
            if ("from".equals(key) && metadata.containsKey("estadoAnterior")) continue;
            if ("to".equals(key) && metadata.containsKey("estadoNuevo")) continue;
            if ("motivoActualizacion".equals(key) && metadata.containsKey("motivo")) continue;
            Object value = en.getValue();
            if (value == null) continue;
            if (value instanceof String s && s.isBlank()) continue;
            String formatted = formatEntry(key, value);
            if (formatted == null || formatted.isBlank()) continue;
            if (sb.length() > 0) sb.append(" | ");
            sb.append(formatted);
        }
        return sb.toString();
    }

    private static String formatEntry(String key, Object value) {
        if ("changes".equals(key) && value instanceof Collection<?> col) {
            String lines = formatChanges(col);
            return lines.isBlank() ? null : keyLabel(key) + ": " + lines;
        }
        String display = formatValue(key, value);
        if (display == null || display.isBlank()) return null;
        return keyLabel(key) + ": " + display;
    }

    private static String formatChanges(Collection<?> col) {
        StringBuilder sb = new StringBuilder();
        for (Object row : col) {
            if (!(row instanceof Map<?, ?> map)) continue;
            Object field = first(map, "fieldLabel", "field");
            String line = String.valueOf(field != null ? field : "Campo")
                    + ": "
                    + displayRaw(map.get("from"))
                    + " → "
                    + displayRaw(map.get("to"));
            if (sb.length() > 0) sb.append("; ");
            sb.append(line);
        }
        return sb.toString();
    }

    private static Object first(Map<?, ?> map, String... keys) {
        for (String k : keys) {
            Object v = map.get(k);
            if (v != null) return v;
        }
        return null;
    }

    private static String formatValue(String key, Object value) {
        if (value instanceof Instant instant) {
            return INSTANT_FMT.format(instant);
        }
        String str = String.valueOf(value).trim();
        if (str.isEmpty() || "null".equalsIgnoreCase(str)) return "";
        Instant parsed = tryParseInstant(str);
        if (parsed != null) {
            return INSTANT_FMT.format(parsed);
        }
        String upper = str.toUpperCase();
        if ("mode".equals(key) && "ZPL_DOWNLOAD".equals(upper)) {
            return "Descarga ZPL";
        }
        if ("exportType".equals(key)) {
            if ("PDF".equals(upper)) return "PDF";
            if ("CSV".equals(upper)) return "CSV";
            if ("EXECUTIVE_DASHBOARD_XLSX".equals(upper)) return "Excel Power BI";
        }
        if ("roleRequested".equals(key) || "rol".equals(key) || "approvalRole".equals(key)) {
            return RoleDisplayTranslator.translate(upper);
        }
        if ("calidadApproved".equals(key) || "inspeccionApproved".equals(key)) {
            if ("TRUE".equals(upper)) return "Sí";
            if ("FALSE".equals(upper)) return "No";
        }
        if ("motivo".equals(key) || "motivoActualizacion".equals(key)) {
            return MOTIVO_LABELS.getOrDefault(upper, str);
        }
        if ("source".equals(key) && "DYNAMICS".equals(upper)) {
            return "Dynamics";
        }
        return str;
    }

    private static String displayRaw(Object value) {
        if (value == null) return "—";
        String s = String.valueOf(value).trim();
        return s.isEmpty() ? "—" : s;
    }

    private static String keyLabel(String key) {
        return KEY_LABELS.getOrDefault(key, key);
    }

    private static Instant tryParseInstant(String raw) {
        try {
            return Instant.parse(raw);
        } catch (Exception ignored) {
            return null;
        }
    }
}
