package com.company.olnaturaqr.support.workflow;

import java.util.Locale;

/**
 * Categorías de material que definen quién puede aprobar.
 */
public final class MaterialType {

    public static final String MATERIA_PRIMA = "MATERIA_PRIMA";
    public static final String EMPAQUE_PRIMARIO = "EMPAQUE_PRIMARIO";
    public static final String EMPAQUE_SECUNDARIO = "EMPAQUE_SECUNDARIO";

    private MaterialType() {}

    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) return "";
        String s = raw.trim().toUpperCase(Locale.ROOT)
                .replace('Á', 'A').replace('É', 'E').replace('Í', 'I')
                .replace('Ó', 'O').replace('Ú', 'U');
        s = s.replaceAll("[^A-Z0-9]+", "_");
        if (s.contains("MATERIA") && s.contains("PRIMA")) return MATERIA_PRIMA;
        if (s.contains("EMPAQUE") && s.contains("PRIMARIO")) return EMPAQUE_PRIMARIO;
        if (s.contains("EMPAQUE") && s.contains("SECUNDARIO")) return EMPAQUE_SECUNDARIO;
        // Códigos de sitio Dynamics (WarehouseId / prefijo de lote)
        if ("MPM".equals(s) || "MPS".equals(s) || MATERIA_PRIMA.equals(s) || "MP".equals(s)) {
            return MATERIA_PRIMA;
        }
        if (EMPAQUE_PRIMARIO.equals(s) || "MEP".equals(s)) return EMPAQUE_PRIMARIO;
        if (EMPAQUE_SECUNDARIO.equals(s)) return EMPAQUE_SECUNDARIO;
        // MEM / MES = familia empaque; la categoría primaria/secundaria la elige el operador.
        return s;
    }

    /** Familia Dynamics: MPM/MPS → materia prima; MEM/MES → empaque (elige primario/secundario). */
    public static String dynamicsFamily(String warehouseOrCode) {
        if (warehouseOrCode == null || warehouseOrCode.isBlank()) return "DESCONOCIDO";
        String s = warehouseOrCode.trim().toUpperCase(Locale.ROOT);
        if ("MPM".equals(s) || "MPS".equals(s)) return "MATERIA_PRIMA";
        if ("MEM".equals(s) || "MES".equals(s)) return "EMPAQUE";
        return "DESCONOCIDO";
    }

    public static boolean isValid(String raw) {
        String n = normalize(raw);
        return MATERIA_PRIMA.equals(n) || EMPAQUE_PRIMARIO.equals(n) || EMPAQUE_SECUNDARIO.equals(n);
    }

    public static String display(String raw) {
        return switch (normalize(raw)) {
            case MATERIA_PRIMA -> "Materia Prima";
            case EMPAQUE_PRIMARIO -> "Material de Empaque Primario";
            case EMPAQUE_SECUNDARIO -> "Material de Empaque Secundario";
            default -> raw == null || raw.isBlank() ? "—" : raw;
        };
    }

    public static boolean requiresCalidad(String raw) {
        String n = normalize(raw);
        return MATERIA_PRIMA.equals(n) || EMPAQUE_PRIMARIO.equals(n);
    }

    public static boolean requiresInspeccion(String raw) {
        String n = normalize(raw);
        return EMPAQUE_SECUNDARIO.equals(n) || EMPAQUE_PRIMARIO.equals(n);
    }
}
