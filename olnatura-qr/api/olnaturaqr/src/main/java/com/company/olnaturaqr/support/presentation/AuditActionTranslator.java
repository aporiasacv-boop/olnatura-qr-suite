package com.company.olnaturaqr.support.presentation;

import java.util.LinkedHashMap;
import java.util.Map;


public final class AuditActionTranslator {

    private static final Map<String, String> LABELS = new LinkedHashMap<>();

    static {
        LABELS.put("PRINT_LABEL", "Impresión de etiquetas");
        LABELS.put("GENERATE_LABEL", "Generación de etiquetas");
        LABELS.put("SCAN_QR", "Consulta QR");
        LABELS.put("SCAN", "Consulta QR");
        LABELS.put("LOGIN_SUCCESS", "Inicio de sesión");
        LABELS.put("LOGOUT", "Cierre de sesión");
        LABELS.put("EXPORT_AUDIT_PDF", "Exportación de auditoría (PDF)");
        LABELS.put("EXPORT_AUDIT_CSV", "Exportación de auditoría (CSV)");
        LABELS.put("EXPORT_USERS_PDF", "Exportación de usuarios (PDF)");
        LABELS.put("EXPORT_EXECUTIVE_DASHBOARD", "Exportación de dashboard ejecutivo");
        LABELS.put("ADD_LOTE_COMMENT", "Comentario agregado al lote");
        LABELS.put("ADMIN_CORRECT_LABEL", "Corrección administrativa");
        LABELS.put("ADMIN_CORRECT_STATUS", "Corrección administrativa de estado");
        LABELS.put("ADMIN_ALIGN_LABELS_DYNAMICS", "Alineación masiva con Dynamics");
        LABELS.put("ADMIN_ALIGN_LABEL_DYNAMICS", "Alineación de lote con Dynamics");
        LABELS.put("SYNC_OPERATIONAL_STATUS_DYNAMICS", "Sincronización de estado operativo (Dynamics)");
        LABELS.put("PURGE_OPERATIONAL_DATA_V2", "Vaciado operativo de BD");
        LABELS.put("PURGE_LOTS", "Eliminación de lotes");
        LABELS.put("CHANGE_STATUS", "Cambio de estado");
        LABELS.put("CHANGE_LOT_ADMIN_STATUS", "Cambio de estado administrativo del lote");
        LABELS.put("ELIMINAR_LOTE", "Eliminar lote");
        LABELS.put("APPROVE_USER", "Aprobación de usuario");
        LABELS.put("REJECT_USER", "Rechazo de usuario");
        LABELS.put("ACCESS_REQUEST", "Solicitud de acceso");
        LABELS.put("DOWNLOAD_LABEL", "Descarga de etiqueta");
        LABELS.put("APPROVE_MATERIAL", "Aprobación de material");
        LABELS.put("REJECT_MATERIAL", "Rechazo de material");
        LABELS.put("UPDATE_USER", "Actualización de usuario");
        LABELS.put("RESET_USER_PASSWORD", "Restablecimiento de contraseña");
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
