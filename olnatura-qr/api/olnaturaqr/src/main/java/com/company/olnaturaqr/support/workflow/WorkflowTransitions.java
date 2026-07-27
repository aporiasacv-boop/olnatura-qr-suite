package com.company.olnaturaqr.support.workflow;

import java.util.Collections;
import java.util.List;

public final class WorkflowTransitions {

    private static final List<String> FROM_CUARENTENA = List.of(
            WorkflowStatus.APROBADO,
            WorkflowStatus.RECHAZADO
    );

    private WorkflowTransitions() {}

    /** Transiciones permitidas. APROBADO y RECHAZADO son terminales (sin salida). */
    public static List<String> allowedFrom(String currentStatus) {
        String normalized = WorkflowStatus.normalize(currentStatus);
        if (WorkflowStatus.CUARENTENA.equals(normalized)) {
            return FROM_CUARENTENA;
        }
        // APROBADO / RECHAZADO: no se puede volver a CUARENTENA ni cruzar entre sí.
        return Collections.emptyList();
    }
}
