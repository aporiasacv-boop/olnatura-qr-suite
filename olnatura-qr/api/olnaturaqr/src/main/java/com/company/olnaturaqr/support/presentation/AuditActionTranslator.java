package com.company.olnaturaqr.support.presentation;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Traducción centralizada de tipos de acción de auditoría para presentación (UI, PDF, CSV, exportaciones).
 * Los valores internos en BD y eventos no se modifican.
 */
public final class AuditActionTranslator {

    private static final Map<String, String> LABELS = new LinkedHashMap<>();

    static {
        LABELS.put("PRINT_LABEL", "Impresión de etiquetas");
        LABELS.put("GENERATE_LABEL", "Generación de etiquetas");
        LABELS.put("SCAN_QR", "Escaneo de código QR");
        LABELS.put("SCAN", "Escaneo de código QR");
        LABELS.put("LOGIN_SUCCESS", "Inicio de sesión");
        LABELS.put("LOGOUT", "Cierre de sesión");
        LABELS.put("EXPORT_AUDIT_PDF", "Exportación de auditoría (PDF)");
        LABELS.put("EXPORT_AUDIT_CSV", "Exportación de auditoría (CSV)");
        LABELS.put("EXPORT_EXECUTIVE_DASHBOARD", "Exportación de dashboard ejecutivo");
        LABELS.put("ADD_LOTE_COMMENT", "Comentario agregado al lote");
        LABELS.put("ADMIN_CORRECT_LABEL", "Corrección administrativa");
        LABELS.put("ADMIN_CORRECT_STATUS", "Corrección administrativa de estado");
        LABELS.put("CHANGE_STATUS", "Cambio de estado");
        LABELS.put("CHANGE_LOT_ADMIN_STATUS", "Cambio de estado administrativo del lote");
        LABELS.put("APPROVE_USER", "Aprobación de usuario");
        LABELS.put("REJECT_USER", "Rechazo de usuario");
        LABELS.put("ACCESS_REQUEST", "Solicitud de acceso");
        LABELS.put("DOWNLOAD_LABEL", "Descarga de etiqueta");
        LABELS.put("APPROVE_MATERIAL", "Aprobación de material");
        LABELS.put("REJECT_MATERIAL", "Rechazo de material");
        LABELS.put("UPDATE_USER", "Actualización de usuario");
    }

    private AuditActionTranslator() {
    }

    public static String translate(String actionType) {
        if (actionType == null || actionType.isBlank()) {
            return "—";
        }
        String key = actionType.trim().toUpperCase();
        return LABELS.getOrDefault(key, actionType.trim());
    }

    public static Map<String, String> allTranslations() {
        return Map.copyOf(LABELS);
    }
}
