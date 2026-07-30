package com.company.olnaturaqr.support.workflow;

import java.util.Locale;


public final class OperationalStatusPresentation {

    private OperationalStatusPresentation() {}

    /**
     * Presentación UI del Estado Operativo ya resuelto por {@link OperationalStatusResolver}.
     * No altera la lógica de negocio: solo mapea {@code DESCONOCIDO}/vacío a {@code CUARENTENA}
     * cuando el negocio lo trata como cuarentena operativa.
     */
    public static String forUi(String resolvedOperationalStatus) {
        if (resolvedOperationalStatus == null || resolvedOperationalStatus.isBlank()) {
            return OperationalStatusResolver.STATUS_CUARENTENA;
        }
        String s = resolvedOperationalStatus.trim().toUpperCase(Locale.ROOT);
        if (OperationalStatusResolver.STATUS_APROBADO.equals(s)
                || OperationalStatusResolver.STATUS_RECHAZADO.equals(s)
                || OperationalStatusResolver.STATUS_CUARENTENA.equals(s)) {
            return s;
        }
        return OperationalStatusResolver.STATUS_CUARENTENA;
    }
}
