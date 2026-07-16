package com.company.olnaturaqr.support.workflow;

import java.util.Collections;
import java.util.List;

public final class WorkflowTransitions {

    private static final List<String> FROM_CUARENTENA = List.of(
            WorkflowStatus.APROBADO,
            WorkflowStatus.RECHAZADO
    );

    private WorkflowTransitions() {}

    /** Transiciones de estado agregadas (UI legacy). La aprobación real usa ApprovalService. */
    public static List<String> allowedFrom(String currentStatus) {
        String normalized = WorkflowStatus.normalize(currentStatus);
        if (WorkflowStatus.CUARENTENA.equals(normalized)) {
            return FROM_CUARENTENA;
        }
        return Collections.emptyList();
    }
}
