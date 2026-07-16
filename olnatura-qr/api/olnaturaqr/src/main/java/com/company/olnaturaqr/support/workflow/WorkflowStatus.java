package com.company.olnaturaqr.support.workflow;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Estados de calidad del material (independientes de Dynamics).
 * CUARENTENA = inicial; APROBADO / RECHAZADO = finales operativos.
 */
public final class WorkflowStatus {

    public static final String CUARENTENA = "CUARENTENA";
    public static final String APROBADO = "APROBADO";
    public static final String RECHAZADO = "RECHAZADO";

    /** Legados — se normalizan a CUARENTENA. */
    public static final String PENDING = "PENDING";
    public static final String LIBERADO = "LIBERADO";
    public static final String DESCONOCIDO = "DESCONOCIDO";

    private static final List<String> CANONICAL = Arrays.asList(CUARENTENA, APROBADO, RECHAZADO);

    private WorkflowStatus() {}

    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) return CUARENTENA;
        String s = raw.trim().toUpperCase(Locale.ROOT);
        if (APROBADO.equals(s)) return APROBADO;
        if (RECHAZADO.equals(s)) return RECHAZADO;
        if (CUARENTENA.equals(s)) return CUARENTENA;
        // PENDING / LIBERADO / DESCONOCIDO / Open / otros → cuarentena operativa
        return CUARENTENA;
    }

    public static boolean isValid(String raw) {
        if (raw == null || raw.isBlank()) return false;
        String s = raw.trim().toUpperCase(Locale.ROOT);
        return CANONICAL.contains(s);
    }

    public static boolean isTerminal(String raw) {
        String s = normalize(raw);
        return APROBADO.equals(s) || RECHAZADO.equals(s);
    }
}
