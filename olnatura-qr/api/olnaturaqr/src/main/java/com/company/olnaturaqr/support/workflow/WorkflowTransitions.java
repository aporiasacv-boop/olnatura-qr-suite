package com.company.olnaturaqr.support.workflow;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;


public final class WorkflowTransitions {

    private static final List<String> ALL = Arrays.asList(
            WorkflowStatus.PENDING,
            WorkflowStatus.APROBADO,
            WorkflowStatus.LIBERADO,
            WorkflowStatus.RECHAZADO,
            WorkflowStatus.CUARENTENA,
            WorkflowStatus.DESCONOCIDO);

    private WorkflowTransitions() {
    }


    public static List<String> allowedFrom(String currentStatus) {
        String normalized = WorkflowStatus.normalize(currentStatus);
        if (normalized.equals(WorkflowStatus.DESCONOCIDO)) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(ALL);
    }
}
