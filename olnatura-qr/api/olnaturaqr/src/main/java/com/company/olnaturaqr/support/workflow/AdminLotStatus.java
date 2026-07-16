package com.company.olnaturaqr.support.workflow;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Ciclo de vida administrativo del lote (independiente del workflow de calidad {@link WorkflowStatus}).
 */
public final class AdminLotStatus {

    public static final String ACTIVE = "ACTIVE";
    public static final String INACTIVE = "INACTIVE";
    public static final String BAJA = "BAJA";

    private static final List<String> VALID = Arrays.asList(ACTIVE, INACTIVE, BAJA);

    private AdminLotStatus() {}

    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) return ACTIVE;
        String s = raw.trim().toUpperCase(Locale.ROOT);
        return VALID.contains(s) ? s : ACTIVE;
    }

    public static boolean isValid(String raw) {
        if (raw == null || raw.isBlank()) return false;
        return VALID.contains(raw.trim().toUpperCase(Locale.ROOT));
    }

    public static boolean isOperational(String raw) {
        return ACTIVE.equals(normalize(raw));
    }

    public static String display(String raw) {
        return switch (normalize(raw)) {
            case INACTIVE -> "INACTIVO";
            case BAJA -> "BAJA";
            default -> "ACTIVO";
        };
    }
}
