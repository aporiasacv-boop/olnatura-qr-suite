package com.company.olnaturaqr.support.presentation;

import java.util.Map;

/**
 * Traducción centralizada de roles internos para presentación al usuario.
 */
public final class RoleDisplayTranslator {

    private static final Map<String, String> LABELS = Map.of(
            "ADMIN", "Administrador",
            "CALIDAD", "Calidad",
            "INSPECCION", "Inspección",
            "ALMACEN", "Almacén",
            "PRODUCCION", "Producción"
    );

    private RoleDisplayTranslator() {
    }

    public static String translate(String role) {
        if (role == null || role.isBlank()) {
            return "—";
        }
        String key = role.trim().toUpperCase();
        return LABELS.getOrDefault(key, role.trim());
    }
}
