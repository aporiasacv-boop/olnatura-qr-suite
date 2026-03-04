package com.company.olnaturaqr.support.workflow;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Valid workflow statuses (backend values in English).
 * Used for validation and as source of truth.
 */
public final class WorkflowStatus {

    public static final String PENDING = "PENDING";
    public static final String APROBADO = "APROBADO";
    public static final String LIBERADO = "LIBERADO";
    public static final String RECHAZADO = "RECHAZADO";
    public static final String CUARENTENA = "CUARENTENA";
    public static final String DESCONOCIDO = "DESCONOCIDO";

    private static final List<String> VALID = Arrays.asList(
            PENDING, APROBADO, LIBERADO, RECHAZADO, CUARENTENA, DESCONOCIDO
    );

    private WorkflowStatus() {}

    /** Returns normalized status or DESCONOCIDO if invalid. */
    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) return DESCONOCIDO;
        String s = raw.trim().toUpperCase(Locale.ROOT);
        return VALID.contains(s) ? s : DESCONOCIDO;
    }

    /** Returns true if the raw string is a valid backend status. */
    public static boolean isValid(String raw) {
        if (raw == null || raw.isBlank()) return false;
        return VALID.contains(raw.trim().toUpperCase(Locale.ROOT));
    }
}
