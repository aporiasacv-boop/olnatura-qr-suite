package com.company.olnaturaqr.support.workflow;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;


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

    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) return DESCONOCIDO;
        String s = raw.trim().toUpperCase(Locale.ROOT);
        return VALID.contains(s) ? s : DESCONOCIDO;
    }


    public static boolean isValid(String raw) {
        if (raw == null || raw.isBlank()) return false;
        return VALID.contains(raw.trim().toUpperCase(Locale.ROOT));
    }
}
